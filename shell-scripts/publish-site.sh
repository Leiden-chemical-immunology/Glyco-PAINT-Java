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

# === Resolve script location ===
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# === CONFIG ===
SITE_DIR="target/site"
BRANCH="gh-pages"
MAIN_BRANCH="main"
TMP_DIR="$(mktemp -d)"   # ✅ safe temporary worktree directory

# === BUILD SITE IF NEEDED ===
if [ ! -d "$SITE_DIR" ]; then
  echo "🏗️  Building Maven site..."
  mvn clean site
fi

# === CREATE WORKTREE FOR GH-PAGES BRANCH ===
echo "🌿 Preparing temporary worktree for '$BRANCH'..."
git fetch origin "$BRANCH" || true
git worktree add "$TMP_DIR" "$BRANCH" 2>/dev/null || git worktree add -b "$BRANCH" "$TMP_DIR"

# === COPY NEW SITE CONTENT ===
echo "📂 Copying new site content..."
rm -rf "$TMP_DIR"/*
cp -R "$SITE_DIR"/* "$TMP_DIR"/

# === COMMIT AND PUSH CHANGES ===
cd "$TMP_DIR"
git add .
git commit -m "Update Maven site" || echo "✅ Nothing new to commit."
git push origin "$BRANCH"

# === CLEANUP WORKTREE ===
cd "$PROJECT_ROOT"
git worktree remove "$TMP_DIR" --force
rm -rf "$TMP_DIR"

# === SUCCESS MESSAGE ===
echo
echo "🚀 Site successfully published to GitHub Pages!"
echo "🌍 https://jjabakker.github.io/JavaPaintProjects"
echo "✅ Your working directory was never modified."