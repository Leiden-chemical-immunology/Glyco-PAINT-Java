#!/usr/bin/env bash
set -e

###############################################################################
# recreate-github-release.sh
#
# PURPOSE:
#   Recreate a deleted or corrupted GitHub release (and tag) cleanly.
#
# USE CASE:
#   You deleted a release or need to force a rebuild (e.g., GitHub Actions failed).
#   This script removes the old release and tag, rebuilds the project,
#   re-tags the latest commit, and pushes it to trigger a fresh release workflow.
#
# ACTIONS PERFORMED:
#   1 Deletes the existing GitHub release and tag (local + remote)
#   2 Rebuilds the project with Maven to ensure it compiles
#   3 Creates a new annotated tag from the latest main commit
#   4 Pushes the new tag to GitHub (triggering GitHub Actions)
#   5 Optionally rebuilds and publishes the Maven site to GitHub Pages
#
# USAGE:
#   ./shell-scripts/recreate-github-release.sh v1.3.0
#
# REQUIREMENTS:
#   - GitHub CLI (`gh`) installed and authenticated
#   - Valid `publish-site.sh` script in `shell-scripts/`
#   - GitHub Actions workflow configured for tagged releases
#
# RESULT:
#   - GitHub release “v1.3.0” re-created and published
#   - Optional: updated Maven site on https://jjabakker.github.io/JavaPaintProjects
#
###############################################################################

# === CONFIGURATION ===
REPO="jjabakker/JavaPaintProjects"   # ✅ your GitHub repository
MAIN_BRANCH="main"
PUBLISH_SITE_SCRIPT="./shell-scripts/publish-site.sh"  # path to your publish script

###############################################################################
# ⚙️  ARGUMENT VALIDATION
###############################################################################

if [ -z "$1" ]; then
  echo "Usage: $0 <tag>"
  echo "Example: $0 v1.3.0"
  exit 1
fi

TAG="$1"

echo "🔁 Recreating release '$TAG' in repository $REPO"
echo "------------------------------------------------------"
read -p "⚠️  This will permanently delete and recreate tag '$TAG'. Continue? (y/n): " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "❌ Aborted."; exit 0; }

###############################################################################
# 🧭  BRANCH VALIDATION
###############################################################################

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$MAIN_BRANCH" ]; then
  echo "⚠️  You are on '$CURRENT_BRANCH'. Switching to '$MAIN_BRANCH'..."
  git switch "$MAIN_BRANCH"
fi

###############################################################################
# 🗑️  STEP 1: DELETE EXISTING RELEASE + TAGS
###############################################################################

if command -v gh >/dev/null 2>&1; then
  echo "🗑️  Deleting GitHub release..."
  gh release delete "$TAG" --repo "$REPO" --yes || echo "(no GitHub release found)"
else
  echo "⚠️  GitHub CLI (gh) not installed — skipping release deletion."
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "🗑️  Deleting local tag..."
  git tag -d "$TAG"
else
  echo "(no local tag found)"
fi

echo "🗑️  Deleting remote tag (if exists)..."
git push origin --delete "$TAG" || echo "(no remote tag found)"

###############################################################################
# 🔧  STEP 2: REBUILD PROJECT
###############################################################################

echo "🔧 Building project to ensure it compiles cleanly..."
mvn clean verify -DskipTests

###############################################################################
# 🏷️  STEP 3: CREATE NEW TAG
###############################################################################

echo "🏷️  Creating new tag '$TAG' from latest $MAIN_BRANCH commit..."
git tag -a "$TAG" -m "Recreated release $TAG"

###############################################################################
# 🚀  STEP 4: PUSH TAG TO TRIGGER GITHUB ACTIONS
###############################################################################

echo "🚀 Pushing tag to origin to trigger release workflow..."
git push origin "$TAG"

echo "✅ Release '$TAG' has been recreated."
echo "💡 GitHub Actions will now rebuild and publish it automatically."

###############################################################################
# 🌍  STEP 5: OPTIONAL MAVEN SITE PUBLISH
###############################################################################

read -p "🌍 Do you also want to rebuild and publish the Maven site to GitHub Pages? (y/n): " SITE_CONFIRM
if [[ "$SITE_CONFIRM" =~ ^[Yy]$ ]]; then
  echo "🧱 Rebuilding Maven site..."
  mvn clean site

  if [ -x "$PUBLISH_SITE_SCRIPT" ]; then
    echo "📤 Publishing Maven site via $PUBLISH_SITE_SCRIPT..."
    "$PUBLISH_SITE_SCRIPT"
  else
    echo "⚠️  Site publish script not found or not executable: $PUBLISH_SITE_SCRIPT"
  fi
else
  echo "⏭️  Skipping Maven site publishing."
fi

###############################################################################
# ✅  WRAP-UP
###############################################################################

echo ""
echo "✅ All done!"
echo "🔹 Release '$TAG' re-published and site optionally updated."
echo "🔹 Check your release here:"
echo "   🔗 https://github.com/$REPO/releases/tag/$TAG"
echo "🔹 Site (if published):"
echo "   🔗 https://jjabakker.github.io/JavaPaintProjects"