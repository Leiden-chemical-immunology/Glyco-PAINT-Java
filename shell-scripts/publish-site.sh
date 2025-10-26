#!/bin/bash
#
###############################################################################
# publish-site.sh
#
# PURPOSE:
#   Builds and publishes the Maven-generated documentation site (target/site)
#   to the `gh-pages` branch of this repository, which serves GitHub Pages.
#
# USE CASE:
#   Run this script after tagging or releasing a new version. It safely rebuilds
#   and deploys the Maven site without disturbing your main working tree or
#   local branches.
#
# ACTIONS PERFORMED:
#   1  Detects project root automatically (can run from anywhere)
#   2  Removes any stale `gh-pages` worktree or branch
#   3  Builds Maven site if missing (`mvn clean site`)
#   4  Creates a temporary worktree for the `gh-pages` branch
#   5  Copies site files from `target/site` into the worktree
#   6  Commits and pushes changes to GitHub Pages
#   7  Cleans up all temporary files and worktrees
#
# FEATURES:
#   • Fully self-contained — no external dependencies beyond Git + Maven
#   • Safe — never modifies your main working tree
#   • Idempotent — does nothing if no site changes are detected
#
# USAGE:
#   chmod +x publish-site.sh
#   ./shell-scripts/publish-site.sh
#
# REQUIREMENTS:
#   - Maven installed and available on PATH
#   - Git configured with push access to the repository
#
# SAFETY FEATURES:
#   - Uses temporary directory for `gh-pages` updates
#   - Automatically deletes stale branches and worktrees
#   - Commits only when differences are detected
#
# RESULT:
#   - The site is deployed to the `gh-pages` branch on GitHub.
#   - Accessible at:
#       🔹 https://jjabakker.github.io/JavaPaintProjects/
#
# WHERE TO CHECK RESULTS:
#   🔹 GitHub Pages site:
#        https://jjabakker.github.io/JavaPaintProjects/
#        → Confirm that site reflects latest Maven documentation
#
#   🔹 GitHub Actions (optional):
#        https://github.com/jjabakker/JavaPaintProjects/actions
#        → Verify that Pages deployment was successful (if using CI)
#
###############################################################################

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