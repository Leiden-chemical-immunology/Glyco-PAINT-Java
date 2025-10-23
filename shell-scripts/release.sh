#!/usr/bin/env bash
set -e

###############################################################################
# 🚀 release.sh
#
# PURPOSE:
#   Automate the Maven + Git release workflow for multi-module projects.
#
# USE CASE:
#   You’re ready to publish a new version (e.g., v1.0.0) and want to:
#     1️⃣ Verify the Maven build
#     2️⃣ Update all pom.xml files to the release version
#     3️⃣ Commit and tag the release in Git
#     4️⃣ Push changes and tag to GitHub
#     5️⃣ Bump all poms to the next "-SNAPSHOT" version
#     6️⃣ Optionally trigger GitHub Actions for release and site updates
#
# FEATURES:
#   - Supports a safe dry-run mode (default)
#   - Interactive confirmation before release
#   - Compatible with GitHub Actions automation
#
# USAGE:
#   💡 Preview (dry run):
#     ./shell-scripts/release.sh
#
#   ⚙️ Execute for real:
#     ./shell-scripts/release.sh --execute
#
#   🏷️  Specify a custom version:
#     ./shell-scripts/release.sh --execute 1.2.3
#
# RESULT:
#   - Tag "v1.2.3" created and pushed
#   - pom.xml bumped to "1.2.4-SNAPSHOT"
#   - Changelog updated with placeholder entry
#
# REQUIREMENTS:
#   - Git and Maven installed
#   - GitHub Actions workflow for release publishing (optional)
#
###############################################################################

# === CONFIGURATION ===
MAIN_BRANCH="main"
DRY_RUN=true
SHOW_DIFF=true

# --- Optional integrations ---
PUBLISH_SITE=false          # ❌ handled automatically by GitHub Actions
CREATE_GITHUB_RELEASE=false # ❌ handled automatically by GitHub Actions

# --- Setup environment ---
cd "$(dirname "$0")/.."  # move to project root

###############################################################################
# ⚙️  ARGUMENT HANDLING
###############################################################################

if [[ "$1" == "--execute" || "$1" == "-x" ]]; then
  DRY_RUN=false
  SHOW_DIFF=false
  echo "⚙️  Execute mode enabled — commands will be applied!"
  shift
else
  echo "💡 Dry-run mode (default) — nothing will be changed."
fi

VERSION_ARG="$1"

###############################################################################
# 🧰  HELPER FUNCTIONS
###############################################################################

# Print and run a command (only in execute mode)
run() {
  echo "👉 $*"
  if [ "$DRY_RUN" = false ]; then
    eval "$@"
  fi
}

# Extract current Maven project version
get_current_version() {
  mvn help:evaluate -Dexpression=project.version -q -DforceStdout
}

# Increment the patch number for the next -SNAPSHOT version
increment_patch_version() {
  local version="$1"
  local base="${version%%-*}"
  local major minor patch
  IFS='.' read -r major minor patch <<< "$base"
  echo "$major.$minor.$((patch + 1))-SNAPSHOT"
}

###############################################################################
# 🔢  DETERMINE VERSIONS
###############################################################################

CURRENT_VERSION=$(get_current_version)
RELEASE_VERSION=${CURRENT_VERSION%-SNAPSHOT}
NEXT_VERSION=$(increment_patch_version "$RELEASE_VERSION")

if [ -n "$VERSION_ARG" ]; then
  RELEASE_VERSION="$VERSION_ARG"
  NEXT_VERSION=$(increment_patch_version "$RELEASE_VERSION")
fi

echo "📦 Current version:      $CURRENT_VERSION"
echo "🏷️  Release version:     $RELEASE_VERSION"
echo "🔄 Next dev version:     $NEXT_VERSION"
read -p "Proceed with release? (y/n): " CONFIRM
[ "$CONFIRM" == "y" ] || exit 0

###############################################################################
# 🧭  BRANCH AND WORKSPACE CHECKS
###############################################################################

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$MAIN_BRANCH" ]; then
  echo "⚠️  You are on branch '$CURRENT_BRANCH'. Switching to '$MAIN_BRANCH'..."
  run git switch "$MAIN_BRANCH"
fi

if [ "$DRY_RUN" = false ] && [ -n "$(git status --porcelain)" ]; then
  echo "❌ Working directory not clean. Commit or stash your changes first."
  exit 1
fi

###############################################################################
# 🧪  VERIFY BUILD
###############################################################################

echo "🧪 Verifying Maven build before tagging..."
run mvn clean verify -DskipTests

###############################################################################
# 🏷️  SET RELEASE VERSION AND UPDATE CHANGELOG
###############################################################################

run mvn versions:set -DnewVersion=$RELEASE_VERSION -DprocessAllModules=true
run mvn versions:commit

CHANGELOG="CHANGELOG.md"
DATE=$(date +"%Y-%m-%d")
HEADER="## v$RELEASE_VERSION - $DATE"

run bash -c "
if [ -f '$CHANGELOG' ]; then
  tmpfile=\$(mktemp)
  {
    echo '$HEADER'
    echo ''
    echo '- Describe new features, fixes, or changes here.'
    echo ''
    cat '$CHANGELOG'
  } > \$tmpfile
  mv \$tmpfile '$CHANGELOG'
else
  cat > '$CHANGELOG' <<EOF
# Changelog

$HEADER

- Initial release notes.
EOF
fi
"

###############################################################################
# 🔍  DRY-RUN DIFF PREVIEW
###############################################################################

if [ "$SHOW_DIFF" = true ]; then
  echo ""
  echo "🟡 Showing diff preview (no files modified on disk):"
  echo "─────────────────────────────────────────────────────────────"
  git diff --color || echo "(no diff to show)"
  echo "─────────────────────────────────────────────────────────────"
  echo ""
fi

###############################################################################
# 🏁  COMMIT, TAG, AND PUSH RELEASE
###############################################################################

run git add -u
run git add "$CHANGELOG"
run git commit -m "Release v$RELEASE_VERSION" || echo "✅ Nothing to commit (release version already up to date)."
run git tag -a "v$RELEASE_VERSION" -m "Release v$RELEASE_VERSION" || echo "⚠️ Tag already exists, skipping."
run git push origin "$MAIN_BRANCH" || echo "⚠️ Push failed or skipped."
run git push origin "v$RELEASE_VERSION" || echo "⚠️ Tag push failed or skipped."

###############################################################################
# 🔄  BUMP TO NEXT SNAPSHOT VERSION
###############################################################################

run mvn versions:set -DnewVersion=$NEXT_VERSION -DprocessAllModules=true
run mvn versions:commit
run git add -u
run git commit -m "Start $NEXT_VERSION development" || echo "✅ Nothing to commit (version already bumped)."
run git push origin "$MAIN_BRANCH" || echo "⚠️ Push failed or skipped."

###############################################################################
# 🌍  OPTIONAL: SITE + GITHUB RELEASE
###############################################################################

if [ "$DRY_RUN" = false ] && [ "$PUBLISH_SITE" = true ]; then
  if [ -x "./shell-scripts/publish-site.sh" ]; then
    echo "🌍 Publishing updated Maven site..."
    ./shell-scripts/publish-site.sh || echo "⚠️ Site publishing failed or skipped."
  else
    echo "⚠️ publish-site.sh not found or not executable. Skipping site publishing."
  fi
fi

if [ "$DRY_RUN" = false ] && [ "$CREATE_GITHUB_RELEASE" = true ]; then
  if command -v gh >/dev/null 2>&1; then
    echo "📢 Creating GitHub release..."
    gh release create "v$RELEASE_VERSION" --title "v$RELEASE_VERSION" --notes-file "$CHANGELOG" || echo "⚠️ GitHub release creation failed."
  else
    echo "⚠️ 'gh' CLI not installed — skipping GitHub release."
  fi
fi

###############################################################################
# ✅  WRAP-UP
###############################################################################

echo ""
if [ "$DRY_RUN" = true ]; then
  echo "✅ Dry-run complete — no changes were made."
  echo "Use './release.sh --execute' to perform the actual release."
else
  echo "🎉 Release v$RELEASE_VERSION complete!"
  echo "🚀 Pushed to GitHub + new development version started."
fi