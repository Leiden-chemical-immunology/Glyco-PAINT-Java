#!/usr/bin/env bash
set -e

###############################################################################
# delete-github-release.sh
#
# PURPOSE:
#   Completely delete a GitHub release and its associated Git tags
#   (both local and remote).
#
# USE CASE:
#   Run this when you’ve published a bad or incomplete release
#   and want to remove all traces before re-creating it.
#
# ACTIONS PERFORMED:
#   1  Confirms with the user before destructive action
#   2  Deletes the GitHub release via the GitHub CLI (`gh release delete`)
#   3  Deletes the local Git tag
#   4  Deletes the remote Git tag from origin
#
# USAGE:
#   ./shell-scripts/delete-github-release.sh v1.3.0
#
# REQUIREMENTS:
#   - Git installed and configured
#   - GitHub CLI (`gh`) installed and authenticated
#
# RESULT:
#   - The specified release and tag are permanently removed
#   - No changes are made to your Maven project files or pom.xml
#
###############################################################################

# === CONFIGURATION ===
REPO="jjabakker/JavaPaintProjects"  # ✅ Your GitHub repository

###############################################################################
# ⚙️  ARGUMENT VALIDATION
###############################################################################

if [ -z "$1" ]; then
  echo "Usage: $0 <tag>"
  echo "Example: $0 v1.3.0"
  exit 1
fi

TAG="$1"

###############################################################################
# ⚠️  SAFETY CONFIRMATION
###############################################################################

echo "🧹 Preparing to delete release and tag '$TAG' from $REPO"
read -p "⚠️  This will permanently delete release '$TAG' and all related tags. Continue? (y/n): " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "❌ Aborted."; exit 0; }

###############################################################################
# 🗑️  DELETE GITHUB RELEASE
###############################################################################

if command -v gh >/dev/null 2>&1; then
  echo "🗑️  Removing GitHub release..."
  gh release delete "$TAG" --repo "$REPO" --yes || echo "(no GitHub release found)"
else
  echo "⚠️  GitHub CLI (gh) not installed — skipping GitHub release deletion."
fi

###############################################################################
# 🗑️  DELETE LOCAL TAG
###############################################################################

if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "🗑️  Removing local tag..."
  git tag -d "$TAG"
else
  echo "(no local tag found)"
fi

###############################################################################
# 🗑️  DELETE REMOTE TAG
###############################################################################

echo "🗑️  Removing remote tag..."
git push origin --delete "$TAG" || echo "(no remote tag found)"

###############################################################################
# ✅  WRAP-UP
###############################################################################

echo "✅ Fully cleaned up release '$TAG'"
echo ""
echo "🔹 Removed from local and remote repositories."
echo "🔹 Check your GitHub Releases page to verify:"
echo "   🔗 https://github.com/$REPO/releases"