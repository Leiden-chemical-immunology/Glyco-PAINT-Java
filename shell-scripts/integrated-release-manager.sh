#!/opt/homebrew/bin/bash

set -euo pipefail

# Ensure we're running from the project root (one level up from this script)
cd "$(dirname "$0")/.."

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  shift
fi

# =========================
# Config (override via env)
# =========================
APP_TEMPLATES_DIR="${APP_TEMPLATES_DIR:-apps}"          # where the .app templates live
STAGE_ROOT="build"                                      # staging root
DIST_DIR="dist"                                         # final deliverables
PRODUCT_NAME="GlycoPAINT"
ORG_REPO="${ORG_REPO:-Leiden-chemical-immunology/Glyco-PAINT-Java}"  # for info only
GIT_TAG_PREFIX="v"                                      # git tag like v1.0.5

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
# Helper functions
# ================
say() {
  if [[ "$DRY_RUN" == true ]]; then
    printf "\033[1;36m(dry-run) ==>\033[0m %s\n" "$*"
  else
    printf "\033[1;32m==>\033[0m %s\n" "$*"
  fi
}

err() { printf "\033[1;31mERROR:\033[0m %s\n" "$*" >&2; exit 1; }

run() {
  if [[ "$DRY_RUN" == true ]]; then
    say "(dry-run) Would run: $*"
  else
    "$@"
  fi
}

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

# Warn if no shared apps folder, but don’t fail — we’ll use module-local bundles instead.
if [[ ! -d "$APP_TEMPLATES_DIR" ]]; then
  say "No shared app templates dir found ('$APP_TEMPLATES_DIR'); will use module-local .app bundles."
fi

# ===================
# Resolve release ver
# ===================
VERSION_ARG="${1:-}"  # optional: pass explicit version e.g. 1.0.5
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

# Ensure staging directories exist even in dry-run mode (for zip test)
if [[ "$DRY_RUN" == true ]]; then
  mkdir -p "$STAGE_DIR" "$DIST_DIR"
fi

# 1) Fiji plugin jar
say "Collecting Fiji plugin jar…"
PLUGIN_JAR="$(resolve_one_jar "paint-fiji-plugin")"
run mkdir -p "$STAGE_DIR/plugin"
run cp -f "$PLUGIN_JAR" "$STAGE_DIR/plugin/"

# 2) Apps
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

# ==========
# Make .zip
# ==========
ZIP_NAME="${PRODUCT_NAME}-${RELEASE_VERSION}.zip"
say "Creating zip (excluding release scripts): $DIST_DIR/$ZIP_NAME"
(
  cd "$STAGE_ROOT"
  # Exclude any shell-scripts, release scripts, or version manager tools
  run zip -qry "../$DIST_DIR/$ZIP_NAME" "${PRODUCT_NAME}-${RELEASE_VERSION}" \
      -x "*/shell-scripts/*" \
      -x "*integrated-release-manager.sh" \
      -x "*release-manager.sh" \
      -x "*make-glyco-paint-installer.sh"
)

# =======================================
# Make self-extracting installer (.sh)
# =======================================
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

say "Creating target: ${TARGET_ROOT}"
mkdir -p "$TARGET_ROOT"

say "Unzipping into ${TARGET_ROOT}…"
unzip -q "$WORKDIR/payload.zip" -d "$WORKDIR/unzip"

TOP="$(find "$WORKDIR/unzip" -maxdepth 1 -type d -name "${PRODUCT_NAME}-*" | head -n1)"
if [[ -z "$TOP" ]]; then
  echo "Installer internal error: cannot find top dir inside zip." >&2
  exit 1
fi

say "Copying apps…"
rsync -a --delete "$TOP/" "$TARGET_ROOT/"

say "Attempting to install Fiji plugin JAR…"
PLUGIN_JAR="$(find "$TARGET_ROOT/plugin" -name 'paint-fiji-plugin-*-jar-with-dependencies.jar' | head -n1 || true)"
if [[ -n "$PLUGIN_JAR" ]]; then
  POSSIBLE_FIJI_DIRS=(
    "${HOME}/Applications/Fiji.app"
    "/Applications/Fiji.app"
  )
  INSTALLED=false
  for D in "${POSSIBLE_FIJI_DIRS[@]}"; do
    if [[ -d "$D" ]]; then
      mkdir -p "$D/plugins"
      cp -f "$PLUGIN_JAR" "$D/plugins/"
      say "Installed plugin to: $D/plugins/"
      INSTALLED=true
      break
    fi
  done
  if [[ "$INSTALLED" != "true" ]]; then
    warn "Fiji not found. Plugin jar kept in: $TARGET_ROOT/plugin/"
  fi
else
  warn "No plugin jar found inside installer? Skipping plugin copy."
fi

if command -v xattr >/dev/null 2>&1; then
  say "Removing quarantine attributes on installed apps (macOS)…"
  xattr -dr com.apple.quarantine "$TARGET_ROOT" || true
fi

say "Done."
exit 0

__ZIPFILE_BELOW__
EOF

sed -i '' "s|ZIP_BASENAME_REPLACEME|ZIP_BASENAME='${ZIP_NAME}'|g" "$DIST_DIR/$INSTALLER_NAME" 2>/dev/null || \
sed -i "s|ZIP_BASENAME_REPLACEME|ZIP_BASENAME='${ZIP_NAME}'|g" "$DIST_DIR/$INSTALLER_NAME"

if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS BSD base64 requires -i
  run base64 -i "$DIST_DIR/$ZIP_NAME" >> "$DIST_DIR/$INSTALLER_NAME"
else
  run base64 "$DIST_DIR/$ZIP_NAME" >> "$DIST_DIR/$INSTALLER_NAME"
fi
run chmod +x "$DIST_DIR/$INSTALLER_NAME"

say "Installer created."

# =======================
# Commit, tag, and push
# =======================
say "Committing release artifacts (if any)…"
run git add -A || true
if ! git diff --cached --quiet; then
  run git commit -m "Release ${RELEASE_VERSION}"
else
  say "No local changes to commit."
fi

TAG="${GIT_TAG_PREFIX}${RELEASE_VERSION}"
if git rev-parse "$TAG" >/dev/null 2>&1; then
  err "Tag $TAG already exists. Aborting."
fi

say "Tagging $TAG and pushing…"
run git tag -a "$TAG" -m "Release $RELEASE_VERSION"
run git push --follow-tags

say "Tag pushed. GitHub Actions should now build and publish the release + Javadoc."

# ============================
# Bump to next -SNAPSHOT
# ============================
say "Bumping Maven version to next -SNAPSHOT…"
next_snapshot
NEW_VERSION="$(get_version)" mod
say "New development version: $NEW_VERSION"

run git add -A
run git commit -m "Bump to $NEW_VERSION"
run git push

say "All done."
say "Deliverables:"
say " - $DIST_DIR/$ZIP_NAME"
say " - $DIST_DIR/$INSTALLER_NAME"

if [[ "$DRY_RUN" == true ]]; then
  say "Dry-run complete — no changes made."
  exit 0
fi