#!/usr/bin/env bash
set -e

###############################################################################
# 🌀 rollback-create-new-github-release.sh
#
# PURPOSE:
#   Safely undo a GitHub release and its associated Git tag,
#   and revert all Maven modules' versions in pom.xml files
#   back to the corresponding "-SNAPSHOT" version.
#
# USE CASE:
#   You created a release (e.g., v1.0.0), but discovered a mistake
#   and need to revert everything cleanly — locally, on GitHub,
#   and in Maven versioning.
#
# ACTIONS PERFORMED:
#   1️⃣  Deletes the GitHub release (via GitHub CLI `gh`)
#   2️⃣  Deletes the local Git tag
#   3️⃣  Deletes the remote Git tag on origin
#   4️⃣  Resets all pom.xml files to the previous -SNAPSHOT version
#   5️⃣  Commits and pushes the rollback to the main branch
#
# REQUIREMENTS:
#   - The GitHub CLI (`gh`) must be installed and authenticated.
#   - Run this script from within your local clone of the repo.
#
# EXAMPLE:
#   ./shell-scripts/rollback-create-new-github-release.sh v1.0.0
#
# RESULT:
#   - GitHub release "v1.0.0" is deleted
#   - Tag "v1.0.0" removed locally and remotely
#   - All pom.xml → version="1.0.0-SNAPSHOT"
#   - Commit "Rollback to 1.0.0-SNAPSHOT after removing release v1.0.0"
#
###############################################################################

# === CONFIGURATION ===
REPO="jjabakker/JavaPaintProjects"   # ✅ your GitHub repository
MAIN_BRANCH="main"

# === ARGUMENT CHECK ===
if [ -z "$1" ]; then
  echo "Usage: $0 <tag>"
  echo "Example: $0 v1.0.0"
  exit 1
fi

TAG="$1"
PREVIOUS_SNAPSHOT=$(echo "$TAG" | sed -E 's/^v//; s/$/-SNAPSHOT/')

echo "🔁 Rolling back release '$TAG' in $REPO"
echo "➡️  Target rollback version: $PREVIOUS_SNAPSHOT"
echo "------------------------------------------------------"
read -p "⚠️  This will delete release '$TAG' and reset pom.xml to $PREVIOUS_SNAPSHOT. Continue? (y/n): " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "❌ Aborted."; exit 0; }

# === CHECK BRANCH ===
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$MAIN_BRANCH" ]; then
  echo "⚠️  You are on '$CURRENT_BRANCH'. Switching to '$MAIN_BRANCH'..."
  git switch "$MAIN_BRANCH"
fi

###############################################################################
# 🧹 STEP 1: Delete GitHub release and Git tags
###############################################################################

# Delete GitHub release (non-interactive)
if command -v gh >/dev/null 2>&1; then
  echo "🗑️  Deleting GitHub release '$TAG'..."
  gh release delete "$TAG" --repo "$REPO" --yes || echo "(no GitHub release found)"
else
  echo "⚠️  GitHub CLI (gh) not installed — skipping GitHub release deletion."
fi

# Delete local tag
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "🗑️  Deleting local tag..."
  git tag -d "$TAG"
else
  echo "(no local tag found)"
fi

# Delete remote tag
echo "🗑️  Deleting remote tag (if exists)..."
git push origin --delete "$TAG" || echo "(no remote tag found)"

###############################################################################
# 🔧 STEP 2: Roll back Maven versions
###############################################################################

echo "🧱 Resetting Maven project version to $PREVIOUS_SNAPSHOT..."
mvn versions:set -DnewVersion="$PREVIOUS_SNAPSHOT" -DprocessAllModules=true
mvn versions:commit

###############################################################################
# 💾 STEP 3: Commit and push rollback
###############################################################################

git add -u
git commit -m "Rollback to $PREVIOUS_SNAPSHOT after removing release $TAG"
git push origin "$MAIN_BRANCH"

###############################################################################
# ✅ DONE
###############################################################################

echo ""
echo "✅ Rollback complete!"
echo "🔹 GitHub release + tag '$TAG' deleted."
echo "🔹 POMs reset to version $PREVIOUS_SNAPSHOT."
echo "🔹 Changes committed and pushed to '$MAIN_BRANCH'."
echo ""
echo "💡 Tip: If you want to recreate this release after fixing issues, use:"
echo "   ./shell-scripts/recreate-release.sh $TAG"