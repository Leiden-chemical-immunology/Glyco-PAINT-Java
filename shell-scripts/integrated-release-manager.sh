#!/opt/homebrew/bin/bash
###############################################################################
# release-manager.sh
#
# PURPOSE:
#   Automates the full Glyco-PAINT release process — from building all modules
#   to packaging the applications, creating the installer, tagging the release,
#   pushing to GitHub, and bumping the Maven version to the next -SNAPSHOT.
#
# USE CASE:
#   Run this script from the repository root whenever you are ready to publish
#   a new version of Glyco-PAINT. It produces both a distributable .zip archive
#   and a self-extracting Bash installer, then triggers GitHub Actions to
#   publish the release and Maven site automatically.
#
# ACTIONS PERFORMED:
#   1  Validates environment (Bash≥4 via /opt/homebrew/bin/bash, Maven, zip, base64)
#   2  Reads current Maven version and resolves release version (CLI arg or POM)
#   3  Sets all modules to the release version (drops -SNAPSHOT)
#   4  Builds fat JARs for all modules
#   5  Stages apps + Fiji plugin outside the repo to avoid tracking large artifacts
#   6  Creates versioned ZIP and a self-extracting installer (.sh)
#   7  Creates + pushes a Git tag (v<version>) to trigger GitHub Actions
#   8  Bumps all modules to next <x.y.(z+1)>-SNAPSHOT and pushes to main
#
# USAGE:
#   chmod +x release-manager.sh
#   ./release-manager.sh <version>
#   ./release-manager.sh --dry-run
#
# REQUIREMENTS:
#   - macOS with Homebrew Bash (Bash 5+, path used here: /opt/homebrew/bin/bash)
#   - Java 8 + Maven installed
#   - zip, base64, rsync, xattr available
#   - Git remote "origin" configured with push access
#
# SAFETY / DESIGN CHOICES:
#   - set -euo pipefail: fail-hard on any error, undefined var, or pipe error
#   - All build outputs live OUTSIDE the repository (../Glyco-PAINT-builds/*)
#     to avoid bloating history / hitting GitHub size limits.
#   - The installer carries the ZIP payload and can install the Fiji plugin
#     into standard Fiji.app locations (~/Applications, /Applications).
###############################################################################

set -euo pipefail

# --- Always start from repo root (script may be run from subfolders) ----------
cd "$(dirname "$0")/.."

# ------------------------------------------------------------------------------
# CLI flags
#   --dry-run : simulate actions, do not change files or push
# ------------------------------------------------------------------------------
DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  shift
fi

# ------------------------------------------------------------------------------
# Logging helpers
#   say: pretty info line
#   err: hard error + exit
#   run: wrapper to honor DRY_RUN (echo command instead of executing)
# ------------------------------------------------------------------------------
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

# ------------------------------------------------------------------------------
# Maven helpers (declared early so we can use them in the banner too)
# ------------------------------------------------------------------------------
get_version() {
  # Try with daemon cleanup disabled first (avoids occasional hangs),
  # fall back to default evaluate if that fails.
  mvn -q -Dexec.cleanupDaemonThreads=false help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null \
    || mvn -q help:evaluate -Dexpression=project.version -DforceStdout
}

next_snapshot_value() {
  # Computes x.y.(z+1)-SNAPSHOT for a given x.y.z (no output validation here)
  local release="$1"
  local major minor patch
  IFS='.' read -r major minor patch <<< "${release}"
  patch=$((patch + 1))
  echo "${major}.${minor}.${patch}-SNAPSHOT"
}

# ------------------------------------------------------------------------------
# Git / Maven context for the runtime banner
#   - Detect branch and remote URL (normalize to org/repo)
#   - Resolve current and next-SNAPSHOT versions from Maven
# ------------------------------------------------------------------------------
GIT_BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)"
GIT_REMOTE_URL="$(git config --get remote.origin.url 2>/dev/null || echo "")"

if [[ -z "$GIT_REMOTE_URL" ]]; then
  # Fallback (keeps banner useful even if no remote is set)
  GIT_REMOTE_SHORT="Leiden-chemical-immunology/Glyco-PAINT-Java"
  GIT_REMOTE_URL="https://github.com/${GIT_REMOTE_SHORT}.git"
else
  # Normalize ssh/https URL into "org/repo" form.
  # Examples:
  #   git@github.com:org/repo.git  -> org/repo
  #   https://github.com/org/repo  -> org/repo
  GIT_REMOTE_SHORT="$(echo "$GIT_REMOTE_URL" | sed -E 's#(git@|https://)([^/:]+)[:/]([^/.]+/[^.]+)(\.git)?#\3#')"
fi

CURRENT_VERSION="$(get_version || true)"
if [[ "$CURRENT_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+) ]]; then
  NEXT_SNAPSHOT="$(next_snapshot_value "${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}")"
else
  NEXT_SNAPSHOT="unknown"
fi

# ------------------------------------------------------------------------------
# Runtime banner (developer-focused, shows what will happen and where)
# ------------------------------------------------------------------------------
clear
echo "==============================================================================="
echo "   Glyco-PAINT Release Manager"
echo "==============================================================================="
echo "  Automates building, packaging, tagging, and publishing releases"
echo "  for the Glyco-PAINT suite (apps + Fiji plugin)."
echo ""
echo "  • Creates versioned .zip and self-extracting installer"
echo "  • Tags and pushes to GitHub → triggers CI/CD release workflow"
echo "  • Bumps all Maven modules to next -SNAPSHOT version"
echo ""
echo "  Current Maven project version : ${CURRENT_VERSION}"
echo "  Next development version      : ${NEXT_SNAPSHOT}"
echo ""
echo "  Active Git branch             : ${GIT_BRANCH}"
echo "  Remote repository             : ${GIT_REMOTE_URL}"
echo ""
echo "  Relevant sites:"
echo "    🔹 Repository   → https://github.com/${GIT_REMOTE_SHORT}"
echo "    🔹 Actions      → https://github.com/${GIT_REMOTE_SHORT}/actions"
echo "    🔹 Releases     → https://github.com/${GIT_REMOTE_SHORT}/releases"
echo "    🔹 Maven site   → https://${GIT_REMOTE_SHORT/github.io\//}.github.io/${GIT_REMOTE_SHORT#*/}/"
echo "    🔹 Javadoc      → https://${GIT_REMOTE_SHORT/github.io\//}.github.io/${GIT_REMOTE_SHORT#*/}/apidocs/"
echo "==============================================================================="
echo ""
sleep 5

# ------------------------------------------------------------------------------
# Build outputs are staged OUTSIDE the repo to avoid tracking artifacts
# ------------------------------------------------------------------------------
ROOT_DIR="$(pwd)"
OUTSIDE_ROOT="${OUTSIDE_ROOT:-${ROOT_DIR}/../Glyco-PAINT-builds}"
APP_TEMPLATES_DIR="${APP_TEMPLATES_DIR:-apps}"
STAGE_ROOT="${OUTSIDE_ROOT}/build"
DIST_DIR="${OUTSIDE_ROOT}/dist"

# Guard: never let OUTSIDE_ROOT collapse to repo root (accidental recursion)
if [[ "$OUTSIDE_ROOT" == "$ROOT_DIR" ]]; then
  err "Build paths cannot point inside the repo! Set OUTSIDE_ROOT to a directory outside the repo."
fi

# Create staging/packing folders
run mkdir -p "$STAGE_ROOT" "$DIST_DIR"
say "Output directories:"
say "  Build: $STAGE_ROOT"
say "  Dist:  $DIST_DIR"

# ------------------------------------------------------------------------------
# Product + repo metadata for filenames and tagging
# ------------------------------------------------------------------------------
PRODUCT_NAME="Glyco-PAINT"   # Used for ZIP/installer naming and top-level dir in payload
SAFE_NAME="${PRODUCT_NAME}"  # Keep as-is (spaces not used in PRODUCT_NAME)
ORG_REPO="${ORG_REPO:-Leiden-chemical-immunology/Glyco-PAINT-Java}"
GIT_TAG_PREFIX="v"

# ------------------------------------------------------------------------------
# Map of modules → .app names, and modules → fat-jar glob patterns
#   Note: these names must match what your module packaging produces.
# ------------------------------------------------------------------------------
declare -A APP_MAP
APP_MAP["paint-generate-squares"]="Generate Squares.app"
APP_MAP["paint-viewer"]="Viewer.app"
APP_MAP["paint-get-omero"]="Get Omero.app"
APP_MAP["paint-create-experiment"]="Create Experiment.app"

declare -A JAR_PATTERN
JAR_PATTERN["paint-generate-squares"]="paint-generate-squares-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-viewer"]="paint-viewer-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-get-omero"]="paint-get-omero-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-create-experiment"]="paint-create-experiment-*-jar-with-dependencies.jar"
JAR_PATTERN["paint-fiji-plugin"]="paint-fiji-plugin-*-jar-with-dependencies.jar"

# ------------------------------------------------------------------------------
# Helper: resolve a single module's fat JAR path (errors out if not found)
# ------------------------------------------------------------------------------
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

# ------------------------------------------------------------------------------
# Environment sanity checks (before build)
# ------------------------------------------------------------------------------
command -v mvn     >/dev/null || err "Maven not found."
command -v zip     >/dev/null || err "zip not found."
command -v base64  >/dev/null || err "base64 not found."
command -v rsync   >/dev/null || err "rsync not found."
# xattr is macOS-specific; we gate its use later with command -v xattr

if [[ ! -d "$APP_TEMPLATES_DIR" ]]; then
  say "No shared app templates dir found ('$APP_TEMPLATES_DIR'); will use module-local .app bundles."
fi

# ------------------------------------------------------------------------------
# Resolve release version:
#   - If user passes an explicit version arg, use it.
#   - Otherwise, drop '-SNAPSHOT' from the current Maven version.
#   - Refuse to release if it still ends with '-SNAPSHOT'.
# ------------------------------------------------------------------------------
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

# ------------------------------------------------------------------------------
# Update all modules to RELEASE version (remove -SNAPSHOT), no backup poms.
#   NOTE: we do this in-tree; the commit/push happens later.
# ------------------------------------------------------------------------------
say "Setting Maven version to release: $RELEASE_VERSION"
run mvn -q -DprocessAllModules versions:set -DnewVersion="$RELEASE_VERSION" -DgenerateBackupPoms=false

# ------------------------------------------------------------------------------
# Fast multi-module build (skip tests for speed, can toggle via flags)
# ------------------------------------------------------------------------------
say "Building all modules (fat jars)…"
run mvn -T 1C -DskipTests clean package

# ------------------------------------------------------------------------------
# Stage apps + plugin into a single versioned folder ready for packaging
#   Example: <OUTSIDE_ROOT>/build/Glyco-PAINT-0.0.5/
# ------------------------------------------------------------------------------
STAGE_DIR="$STAGE_ROOT/${PRODUCT_NAME}-${RELEASE_VERSION}"
say "Staging into: $STAGE_DIR"
run rm -rf "$STAGE_DIR" "$DIST_DIR"    # Start fresh
run mkdir -p "$STAGE_DIR" "$DIST_DIR"

# 1) Fiji plugin JAR
say "Collecting Fiji plugin jar…"
PLUGIN_JAR_DIR="paint-fiji-plugin/target"
PLUGIN_JAR_PATTERN="paint-fiji-plugin-*-jar-with-dependencies.jar"

PLUGIN_JAR=$(compgen -G "$PLUGIN_JAR_DIR/$PLUGIN_JAR_PATTERN" | head -n1 || true)
if [[ -z "$PLUGIN_JAR" ]]; then
  err "Cannot find Fiji plugin fat jar in $PLUGIN_JAR_DIR"
fi

run mkdir -p "$STAGE_DIR/plugin"
say "Copying: $(basename "$PLUGIN_JAR")"
run cp -f "$PLUGIN_JAR" "$STAGE_DIR/plugin/"

# Keep ONLY the fat plugin jar; delete any stray jars (e.g., paint-shared-utils-*.jar)
run find "$STAGE_DIR/plugin" -maxdepth 1 -type f -name 'paint-*.jar' \
  ! -name 'paint-fiji-plugin-*-jar-with-dependencies.jar' -print -delete

# 2) Each desktop app bundle + module fat JAR → drop JAR into app/Contents/Java/
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

# macOS convenience: strip quarantine recursively to eliminate 'unverified developer' warnings
if command -v xattr >/dev/null 2>&1; then
  say "Removing quarantine attributes (macOS)…"
  run xattr -dr com.apple.quarantine "$STAGE_DIR" || true
fi

# ------------------------------------------------------------------------------
# Create versioned ZIP of the staged tree
#   Packaging excludes shell-scripts/tools (defense in depth; staging shouldn’t contain them)
# ------------------------------------------------------------------------------
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

# ------------------------------------------------------------------------------
# Self-extracting installer (.sh) that:
#   - Unpacks payload ZIP into ~/Applications/Glyco-PAINT
#   - Attempts to install Fiji plugin into standard Fiji.app locations
#   - Removes quarantine attributes (macOS) to aid first-run UX
# ------------------------------------------------------------------------------
INSTALLER_NAME="${PRODUCT_NAME}-Installer-${RELEASE_VERSION}.sh"
say "Creating self-extracting installer: $DIST_DIR/$INSTALLER_NAME"

cat > "$DIST_DIR/$INSTALLER_NAME" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

PRODUCT_NAME="Glyco-PAINT"
TARGET_ROOT="${HOME}/Applications/Glyco-PAINT"

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

say "Unzipping into temporary directory…"
unzip -q "$WORKDIR/payload.zip" -d "$WORKDIR/unzip"

TOP="$(find "$WORKDIR/unzip" -maxdepth 1 -type d -name "${PRODUCT_NAME}-*" | head -n1)"
if [[ -z "$TOP" ]]; then
  echo "Installer internal error: cannot find top dir inside zip." >&2
  exit 1
fi

say "Copying applications…"
rsync -a --delete --exclude 'plugin/' "$TOP/" "$TARGET_ROOT/"

# Clean any leftover from older installers
rm -rf "$TARGET_ROOT/plugin" 2>/dev/null || true

# --- Install Fiji plugin -----------------------------------------------------
say "Installing Fiji plugin JAR…"

# Find exactly one plugin jar from the installer payload (not from Applications)
PLUGIN_JAR="$(find "$TOP" -type f -name 'paint-fiji-plugin-*-jar-with-dependencies.jar' | head -n1 || true)"

if [[ -z "$PLUGIN_JAR" ]]; then
  warn "No plugin jar found in installer payload. Skipping plugin installation."
else
  POSSIBLE_FIJI_DIRS=(
    "${HOME}/Applications/Fiji.app"
    "/Applications/Fiji.app"
  )

  INSTALLED=false
  for D in "${POSSIBLE_FIJI_DIRS[@]}"; do
    if [[ -d "$D" ]]; then
      PLUGINS_DIR="$D/plugins"
      mkdir -p "$PLUGINS_DIR"

      echo ""
      say "Checking for existing PAINT-related jars in: $PLUGINS_DIR"
      OLD_JARS=($(find "$PLUGINS_DIR" -type f -name 'paint-*.jar' 2>/dev/null || true))

      if (( ${#OLD_JARS[@]} > 0 )); then
        echo "Found existing plugin jars:"
        printf '  - %s\n' "${OLD_JARS[@]}"
        echo ""
        read -r -p "Remove old PAINT plugin jars before installing the new one? [y/N] " ANSWER
        if [[ "$ANSWER" =~ ^[Yy]$ ]]; then
          say "Removing old plugin jars..."
          for J in "${OLD_JARS[@]}"; do
            rm -f "$J"
          done
        else
          warn "Keeping old jars; skipping plugin installation for safety."
          continue
        fi
      fi

      say "Copying new plugin jar: $(basename "$PLUGIN_JAR")"
      cp -f "$PLUGIN_JAR" "$PLUGINS_DIR/"
      say "Installed plugin to: $PLUGINS_DIR"

      INSTALLED=true
      break
    fi
  done

  if [[ "$INSTALLED" != "true" ]]; then
    warn "No Fiji.app installation detected. Plugin jar was not installed."
  fi
fi

# --- macOS UX convenience ----------------------------------------------------
if command -v xattr >/dev/null 2>&1; then
  say "Removing quarantine attributes on installed apps (macOS)…"
  xattr -dr com.apple.quarantine "$TARGET_ROOT" || true
fi

say "Installation complete."
echo ""
echo "✅ Applications installed to:"
echo "   $TARGET_ROOT"
echo "If you use Fiji, verify the plugin is present under:"
echo "   Fiji.app/plugins/"
echo ""
exit 0

__ZIPFILE_BELOW__
EOF

# Append the payload ZIP as base64 so installer is standalone.
if [[ "$OSTYPE" == "darwin"* ]]; then
  # BSD base64 requires -i for input file
  run base64 -i "$DIST_DIR/$ZIP_NAME" >> "$DIST_DIR/$INSTALLER_NAME"
else
  run base64 "$DIST_DIR/$ZIP_NAME" >> "$DIST_DIR/$INSTALLER_NAME"
fi
run chmod +x "$DIST_DIR/$INSTALLER_NAME"

say "Installer created."

# ------------------------------------------------------------------------------
# Tag and push the release. This triggers GitHub Actions (on tag 'v*.*.*').
#   We push main with --follow-tags so both branch changes (POMs set to release)
#   and the annotated tag reach the remote.
# ------------------------------------------------------------------------------
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

# ------------------------------------------------------------------------------
# Bump to next -SNAPSHOT for continued development
#   This keeps main ahead immediately after release tagging.
# ------------------------------------------------------------------------------
say "Bumping Maven version to next -SNAPSHOT..."
# Recompute next from RELEASE_VERSION (not CURRENT_VERSION) to avoid drift.
NEXT_DEV="$(next_snapshot_value "$RELEASE_VERSION")"
run mvn -q -DprocessAllModules versions:set -DnewVersion="$NEXT_DEV" -DgenerateBackupPoms=false

say "New development version: $(get_version)"

run git add -A
run git commit -m "Bump to ${NEXT_DEV}"
run git push origin main

say ""
say "✅ All done."
say "Deliverables:"
say "  - $DIST_DIR/$ZIP_NAME"
say "  - $DIST_DIR/$INSTALLER_NAME"
say ""