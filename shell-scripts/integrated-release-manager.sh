#!/opt/homebrew/bin/bash

set -euo pipefail

# Ensure we're running from the project root (one level up from this script)
cd "$(dirname "$0")/.."

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  shift
fi

# ================
# Helper functions
# ================
say() {
  if [[ "$DRY_RUN" == true ]]; then
    echo "(dry-run) ==> $*"
  else
    printf "\033[1;32m==>\033[0m %s\n" "$*"
  fi
}

err() {
  printf "\033[1;31mERROR:\033[0m %s\n" "$*" >&2
  exit 1
}

run() {
  if [[ "$DRY_RUN" == true ]]; then
    say "(dry-run) Would run: $*"
  else
    "$@"
  fi
}

# =========================
# Config
# =========================
ROOT_DIR="$(pwd)"
OUTSIDE_ROOT="${OUTSIDE_ROOT:-${ROOT_DIR}/../Glyco-PAINT-builds}"

APP_TEMPLATES_DIR="${APP_TEMPLATES_DIR:-apps}"
STAGE_ROOT="${OUTSIDE_ROOT}/build"
DIST_DIR="${OUTSIDE_ROOT}/dist"

# Check that OUTSIDE_ROOT is not inside repo
if [[ "$OUTSIDE_ROOT" == "$ROOT_DIR" ]]; then
  err "Build paths cannot point inside the repo! Set OUTSIDE_ROOT to a directory outside the repo."
fi

# Create directories now that say() is defined
mkdir -p "$STAGE_ROOT" "$DIST_DIR"
say "Output directories:"
say "  Build: $STAGE_ROOT"
say "  Dist:  $DIST_DIR"

PRODUCT_NAME="GlycoPAINT"
SAFE_NAME="${PRODUCT_NAME}"
ORG_REPO="${ORG_REPO:-Leiden-chemical-immunology/Glyco-PAINT-Java}"
GIT_TAG_PREFIX="v"

# Map module -> app bundle name
declare -A APP_MAP
APP_MAP["paint-generate-squares"]="Generate Squares.app"
APP_MAP["paint-viewer"]="Viewer.app"
APP_MAP["paint-get-omero"]="Get Omero.app"
APP_MAP["paint-create-experiment"]="Create Experiment.app"

# Module -> fat jar pattern (relative to module/target)
declare -A JAR_PATTERN
JAR_PATTERN["paint-generate-squares"]="paint-generate-squares-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-viewer"]="paint-viewer-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-get-omero"]="paint-get-omero-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-create-experiment"]="paint-create-experiment-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-fiji-plugin"]="paint-fiji-plugin-*-jar-with-dependencies.jar"

# ================
# Maven helpers
# ================
get_version() {
  mvn -q -Dexec.cleanupDaemonThreads=false help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null \
    || mvn -q help:evaluate -Dexpression=project.version -DforceStdout
}

next_snapshot() {
  local current release major minor patch next
  current="$(get_version)"
  release="${current/-SNAPSHOT/}"
  IFS='.' read -r major minor patch <<< "$release"
  patch=$((patch + 1))
  next="${major}.${minor}.${patch}-SNAPSHOT"
  say "Setting next development version: $next"
  run mvn -q -DprocessAllModules versions:set \
      -DnewVersion="$next" \
      -DgenerateBackupPoms=false
}

resolve_one_jar() {
  local module="$1"
  local pattern="${JAR_PATTERN[$module]}"
  local dir="$module/target"
  [[ -d "$dir" ]] || err "Missing target dir for $module ($dir). Did build fail?"
  local path
  path=$(compgen -G "$dir/$pattern" | head -n1 || true)
  [[ -n "${path:-}" ]] || err "Cannot find fat JAR for $module (pattern $pattern)"
  echo "$path"
}

# ================
# Pre-flight checks
# ================
command -v mvn >/dev/null || err "Maven not found."
command -v zip >/dev/null || err "zip not found."
command -v base64 >/dev/null || err "base64 not found."

if [[ ! -d "$APP_TEMPLATES_DIR" ]]; then
  say "No shared app templates dir found ('$APP_TEMPLATES_DIR'); will use module-local .app bundles."
fi

# ===================
# Resolve release ver
# ===================
VERSION_ARG="${1:-}"
CURRENT_VERSION="$(get_version)"
[[ -n "$CURRENT_VERSION" ]] || err "Could not resolve current project.version"

if [[ -n "$VERSION_ARG" ]]; then
  RELEASE_VERSION="$VERSION_ARG"
else
  RELEASE_VERSION="${CURRENT_VERSION/-SNAPSHOT/}"
fi

say "Project version: $CURRENT_VERSION"
say "Release version: $RELEASE_VERSION"

if [[ "$RELEASE_VERSION" == *"-SNAPSHOT" ]]; then
  err "Refusing to release a SNAPSHOT ($RELEASE_VERSION). Pass explicit clean version, e.g.: ./release-manager.sh 1.0.5"
fi

# =====================
# Prepare clean release
# =====================
say "Setting Maven version to release: $RELEASE_VERSION"
run mvn -q -DprocessAllModules versions:set -DnewVersion="$RELEASE_VERSION" -DgenerateBackupPoms=false

# =============
# Build modules
# =============
say "Building all modules (fat jars)…"
run mvn -T 1C -DskipTests clean package

# ==========================
# Stage apps + plugin bundle
# ==========================
STAGE_DIR="$STAGE_ROOT/${PRODUCT_NAME}-${RELEASE_VERSION}"
say "Staging into: $STAGE_DIR"
run rm -rf "$STAGE_DIR" "$DIST_DIR"
run mkdir -p "$STAGE_DIR" "$DIST_DIR"

if [[ "$DRY_RUN" == true ]]; then
  mkdir -p "$STAGE_DIR" "$DIST_DIR"
fi

say "Collecting Fiji plugin jar…"
PLUGIN_JAR="$(resolve_one_jar "paint-fiji-plugin")"
run mkdir -p "$STAGE_DIR/plugin"
run cp -f "$PLUGIN_JAR" "$STAGE_DIR/plugin/"

for module in "paint-generate-squares" "paint-viewer" "paint-get-omero" "paint-create-experiment"; do
  app_name="${APP_MAP[$module]}"
  module_app_dir="$module/target/$app_name"
  src_app="$APP_TEMPLATES_DIR/$app_name"

  if [[ -d "$module_app_dir" ]]; then
    say "Found app bundle inside module: $module_app_dir"
    src_app="$module_app_dir"
  elif [[ -d "$src_app" ]]; then
    say "Using shared app template: $src_app"
  else
    err "Missing app bundle for $module (looked in $module_app_dir and $src_app)"
  fi

  say "Preparing app: $app_name"
  run cp -R "$src_app" "$STAGE_DIR/$app_name"
  jar_path="$(resolve_one_jar "$module")"
  run cp -f "$jar_path" "$STAGE_DIR/$app_name/Contents/Java/"
done

if command -v xattr >/dev/null 2>&1; then
  say "Removing quarantine attributes (macOS)…"
  run xattr -dr com.apple.quarantine "$STAGE_DIR" || true
fi

ZIP_NAME="${PRODUCT_NAME}-${RELEASE_VERSION}.zip"
say "Creating zip: $DIST_DIR/$ZIP_NAME"
(
  cd "$STAGE_ROOT"
  run zip -qry "$DIST_DIR/$ZIP_NAME" "${PRODUCT_NAME}-${RELEASE_VERSION}" \
      -x "*/shell-scripts/*" \
      -x "*integrated-release-manager.sh" \
      -x "*release-manager.sh" \
      -x "*make-glyco-paint-installer.sh"
)
INSTALLER_NAME="${PRODUCT_NAME}-Installer-${RELEASE_VERSION}.sh"
say "Creating self-extracting installer: $DIST_DIR/$INSTALLER_NAME"

cat > "$DIST_DIR/$INSTALLER_NAME" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

PRODUCT_NAME="GlycoPAINT"
TARGET_ROOT="${HOME}/Applications/Glyco-PAINT"
ZIP_BASENAME_REPLACEME

say() { printf "\033[1;32m==>\033[0m %s\n" "$*"; }
warn() { printf "\033[1;33mWARN:\033[0m %s\n" "$*\n"; }

SELF="$0"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

say "Extracting payload…"
ARCHIVE_LINE=$(awk '/^__ZIPFILE_BELOW__/ {print NR + 1; exit 0; }' "$SELF")
tail -n +$ARCHIVE_LINE "$SELF" | base64 --decode > "$WORKDIR/payload.zip"
mkdir -p "$TARGET_ROOT"
unzip -q "$WORKDIR/payload.zip" -d "$WORKDIR/unzip"

TOP="$(find "$WORKDIR/unzip" -maxdepth 1 -type d -name "${PRODUCT_NAME}-*" | head -n1)"
rsync -a --delete "$TOP/" "$TARGET_ROOT/"
say "Done."
exit 0
__ZIPFILE_BELOW__
EOF

sed -i '' "s|ZIP_BASENAME_REPLACEME|ZIP_BASENAME='${ZIP_NAME}'|g" "$DIST_DIR/$INSTALLER_NAME" 2>/dev/null || \
sed -i "s|ZIP_BASENAME_REPLACEME|ZIP_BASENAME='${ZIP_NAME}'|g" "$DIST_DIR/$INSTALLER_NAME"

if [[ "$OSTYPE" == "darwin"* ]]; then
  run base64 -i "$DIST_DIR/$ZIP_NAME" >> "$DIST_DIR/$INSTALLER_NAME"
else
  run base64 "$DIST_DIR/$ZIP_NAME" >> "$DIST_DIR/$INSTALLER_NAME"
fi
run chmod +x "$DIST_DIR/$INSTALLER_NAME"

say "Installer created."

TAG="${GIT_TAG_PREFIX}${RELEASE_VERSION}"
say "Tagging release as ${TAG}..."
run git tag -a "$TAG" -m "Release ${RELEASE_VERSION}"
run git push origin main --follow-tags

say ""
say "✅ Tag ${TAG} pushed successfully."
say "GitHub Actions will now automatically build and publish the release."
say "Monitor progress here:"
say "  https://github.com/${ORG_REPO}/actions"
say ""