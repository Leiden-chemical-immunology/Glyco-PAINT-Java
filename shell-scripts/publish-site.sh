#!/bin/bash
#
# ================================================================
#  publish-site.sh
#
#  Description:
#    Builds and publishes the Maven-generated site (target/site)
#    to the `gh-pages` branch of this repository, which powers
#    GitHub Pages hosting.
#
#  Features:
#    • Works from any directory (auto-detects project root)
#    • Creates a temporary Git worktree for gh-pages updates
#    • Builds site automatically if missing
#    • Cleans up temporary files safely
#    • Leaves your main working tree untouched
#
#  Usage:
#    ./shell-scripts/publish-site.sh
#
#  Requirements:
#    - Maven must be installed and on your PATH
#    - Git must be configured with push access
#
#  Author: Hans Bakker (jjabakker)
#  Updated: $(date +"%Y-%m-%d")
# ================================================================

set -e

# === SAFETY CLEANUP ===
echo "🧹 Cleaning up any stale gh-pages worktree or branch..."
if git worktree list | grep -q "gh-pages"; then
  git worktree remove gh-pages --force || true
fi
if git show-ref --verify --quiet refs/heads/gh-pages; then
  git branch -D gh-pages || true
fi

# === Resolve script location ===
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# === CONFIG ===
SITE_DIR="target/site"
BRANCH="gh-pages"
TMP_DIR="$(mktemp -d)"   # ✅ safe temporary worktree directory

# === BUILD SITE IF NEEDED ===
if [ ! -d "$SITE_DIR" ]; then
  echo "🏗️  Building Maven site..."
  mvn clean site
fi

# === FETCH REMOTE BRANCH SAFELY ===
echo "🌿 Preparing temporary worktree for '$BRANCH'..."
git fetch origin "$BRANCH" || true

# Create or checkout branch safely
if git show-ref --verify --quiet "refs/remotes/origin/$BRANCH"; then
  git worktree add "$TMP_DIR" "origin/$BRANCH"
  cd "$TMP_DIR"
  git checkout -B "$BRANCH"
else
  git worktree add -b "$BRANCH" "$TMP_DIR"
fi

# === COPY NEW SITE CONTENT ===
echo "📂 Copying new site content..."
rm -rf "$TMP_DIR"/*
cp -R "$SITE_DIR"/* "$TMP_DIR"/

# === COMMIT AND PUSH CHANGES ===
cd "$TMP_DIR"
git add .
if git diff --cached --quiet; then
  echo "✅ Nothing new to commit."
else
  git commit -m "Update Maven site ($(date +'%Y-%m-%d %H:%M:%S'))"
  git push -u origin "$BRANCH"
fi

# === CLEANUP WORKTREE ===
cd "$PROJECT_ROOT"
git worktree remove "$TMP_DIR" --force
rm -rf "$TMP_DIR"

# === SUCCESS MESSAGE ===
echo
echo "🚀 Site successfully published to GitHub Pages!"
echo "🌍 https://jjabakker.github.io/JavaPaintProjects"
echo "✅ Your working directory was never modified."