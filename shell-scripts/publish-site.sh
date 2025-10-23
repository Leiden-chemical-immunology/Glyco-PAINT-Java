#!/bin/bash
set -e

SITE_DIR="../target/site"
BRANCH="gh-pages"
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

TMP_DIR=$(mktemp -d)
echo "📦 Copying site to temporary folder..."
cp -R target/site/* "$TMP_DIR"

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$BRANCH" ]; then
  echo "⚠️  You are on branch '$CURRENT_BRANCH'."
  read -p "Switch to '$BRANCH' and continue? (y/n) " CONFIRM
  if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo "❌ Aborted."
    exit 1
  fi
  git switch "$BRANCH"
fi

echo "🧹 Cleaning old site files..."
git rm -rf . >/dev/null 2>&1 || true
git clean -fdx >/dev/null 2>&1 || true

echo "📂 Copying new site from temporary folder..."
cp -R "$TMP_DIR"/* .

git add .
git commit -m "Update Maven site" || echo "✅ Nothing new to commit."
git push origin "$BRANCH"

echo "🚀 Site published to GitHub Pages!"
echo "🌍 https://jjabakker.github.io/JavaPaintProjects"

echo "🔄 Returning to '$MAIN_BRANCH'..."
git switch "$MAIN_BRANCH"

echo "🧹 Cleaning temporary data..."
rm -rf "$TMP_DIR"
mvn clean -q || echo "(clean skipped)"

echo "✅ Done! Back on '$MAIN_BRANCH'."