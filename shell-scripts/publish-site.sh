#!/bin/bash
set -e

# === CONFIG ===
SITE_DIR="target/site"
BRANCH="gh-pages"
MAIN_BRANCH="main"     # ← Change if your main branch is named differently

# === SAFETY CHECKS ===
if [ ! -d "../.git" ]; then
  echo "❌ Error: not a Git repository. Run this from the Shell scripts folder inside your project."
  exit 1
fi

if [ ! -d "$SITE_DIR" ]; then
  echo "❌ Error: $SITE_DIR not found. Run 'mvn clean site' in the project root first."
  exit 1
fi

cd ..

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$BRANCH" ]; then
  echo "⚠️  You are on branch '$CURRENT_BRANCH'."
  read -p "Switch to '$BRANCH' and continue? (y/n) " CONFIRM
  if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
    git switch "$BRANCH"
  else
    echo "❌ Aborted."
    exit 1
  fi
fi

# === CLEAN OLD FILES ===
echo "🧹 Cleaning old site files..."
git rm -rf . >/dev/null 2>&1 || true
git clean -fdx >/dev/null 2>&1 || true

# === COPY NEW SITE ===
echo "📂 Copying new site from $SITE_DIR..."
cp -R "$SITE_DIR"/* .

# === COMMIT AND PUSH ===
git add .
git commit -m "Update Maven site" || echo "✅ Nothing new to commit."
git push origin "$BRANCH"

echo "🚀 Site published to GitHub Pages!"
echo "🌍 https://jjabakker.github.io/JavaPaintProjects"

# === POST-PUBLISH CLEANUP ===
echo "🔄 Returning to '$MAIN_BRANCH'..."
git switch "$MAIN_BRANCH"

echo "🧹 Cleaning local build artifacts..."
mvn clean -q || echo "(clean skipped)"

echo "✅ Done! Back on '$MAIN_BRANCH'."