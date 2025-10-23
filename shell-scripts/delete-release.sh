#!/usr/bin/env bash
set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <tag>"
  echo "Example: $0 v1.3.0"
  exit 1
fi

TAG="$1"
REPO="herrdoctor/paint"   # <-- change to your actual repo

echo "🧹 Deleting release and tag '$TAG' from $REPO"

# Delete GitHub release (non-interactive)
gh release delete "$TAG" --repo "$REPO" --yes || echo "(no release found)"

# Delete local tag
if git rev-parse "$TAG" >/dev/null 2>&1; then
  git tag -d "$TAG"
else
  echo "(no local tag found)"
fi

# Delete remote tag
git push origin --delete "$TAG" || echo "(no remote tag found)"

echo "✅ Deleted release and tag: $TAG"