#!/bin/bash
set -e

SITE_DIR="../target/site"
BRANCH="gh-pages"
TMP_DIR="../target/ghpages-tmp"
MAIN_BRANCH="main"

if [ ! -d "../.git" ]; then
  echo "❌ Error: not a Git repository. Run this from the shell-scripts folder inside your project."
  exit 1
fi

cd ..

if [ ! -d "target/site" ]; then
  echo "🏗️ Building site..."
  mvn clean site
fi

# === CREATE WORKTREE FOR GH-PAGES BRANCH ===
echo "🌿 Preparing temporary worktree for '$BRANCH'..."
rm -rf "$TMP_DIR"
git worktree add "$TMP_DIR" "$BRANCH" || git worktree add -b "$BRANCH" "$TMP_DIR"

# === COPY NEW SITE ===
echo "📂 Copying new site..."
rm -rf "$TMP_DIR"/*
cp -R "$SITE_DIR"/* "$TMP_DIR"/

# === COMMIT AND PUSH ===
cd "$TMP_DIR"
git add .
git commit -m "Update Maven site" || echo "✅ Nothing new to commit."
git push origin "$BRANCH"

# === CLEANUP ===
cd ..
git worktree remove "$TMP_DIR" --force
rm -rf "$TMP_DIR"

echo "🚀 Site published successfully to GitHub Pages!"
echo "🌍 https://jjabakker.github.io/JavaPaintProjects"
echo "✅ Your working directory was never modified."