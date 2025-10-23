#!/usr/bin/env bash
set -e

# --- Default configuration ---
DRY_RUN=true
SHOW_DIFF=true

# --- Parse arguments ---
if [[ "$1" == "--execute" || "$1" == "-x" ]]; then
  DRY_RUN=false
  SHOW_DIFF=false
  echo "⚙️  Execute mode enabled — commands will be applied!"
  shift
else
  echo "💡 Dry-run mode (default) — nothing will be changed."
fi

VERSION_ARG="$1"

# --- Helper to conditionally execute commands ---
run() {
  echo "👉 $*"
  if [ "$DRY_RUN" = false ]; then
    eval "$@"
  fi
}

# --- Helper functions ---
get_current_version() {
  mvn help:evaluate -Dexpression=project.version -q -DforceStdout
}

increment_patch_version() {
  local version="$1"
  local base="${version%%-*}"
  local major minor patch
  IFS='.' read -r major minor patch <<< "$base"
  echo "$major.$minor.$((patch + 1))-SNAPSHOT"
}

# --- Determine versions ---
CURRENT_VERSION=$(get_current_version)
RELEASE_VERSION=${CURRENT_VERSION%-SNAPSHOT}
NEXT_VERSION=$(increment_patch_version "$RELEASE_VERSION")

if [ -n "$VERSION_ARG" ]; then
  RELEASE_VERSION="$VERSION_ARG"
  NEXT_VERSION=$(increment_patch_version "$RELEASE_VERSION")
fi

echo "📦 Current version:     $CURRENT_VERSION"
echo "🏷️  Release version:     $RELEASE_VERSION"
echo "🔄 Next dev version:     $NEXT_VERSION"
read -p "Proceed with release? (y/n): " CONFIRM
[ "$CONFIRM" == "y" ] || exit 0

# --- Check for clean working tree ---
if [ "$DRY_RUN" = false ] && [ -n "$(git status --porcelain)" ]; then
  echo "❌ Working directory not clean. Commit or stash your changes first."
  exit 1
fi

# --- Set release version ---
run mvn versions:set -DnewVersion=$RELEASE_VERSION -DprocessAllModules=true
run mvn versions:commit

# --- Update CHANGELOG.md ---
CHANGELOG="CHANGELOG.md"
DATE=$(date +"%Y-%m-%d")
HEADER="## v$RELEASE_VERSION - $DATE"

run bash -c "
if [ -f '$CHANGELOG' ]; then
  tmpfile=\$(mktemp)
  {
    echo '$HEADER'
    echo ''
    echo '- Describe new features, fixes, or changes here.'
    echo ''
    cat '$CHANGELOG'
  } > \$tmpfile
  mv \$tmpfile '$CHANGELOG'
else
  cat > '$CHANGELOG' <<EOF
# Changelog

$HEADER

- Initial release notes.
EOF
fi
"

# --- Diff preview in dry-run mode ---
if [ "$SHOW_DIFF" = true ]; then
  echo ""
  echo "🟡 Showing diff preview (no files modified on disk):"
  echo "─────────────────────────────────────────────────────────────"
  git diff --color || echo "(no diff to show)"
  echo "─────────────────────────────────────────────────────────────"
  echo ""
fi

# --- Commit and tag ---
run git add -u
run git add "$CHANGELOG"
run git commit -m "Release v$RELEASE_VERSION"
run git tag -a "v$RELEASE_VERSION" -m "Release v$RELEASE_VERSION"

# --- Push and bump ---
run git push origin main
run git push origin "v$RELEASE_VERSION"
run mvn versions:set -DnewVersion=$NEXT_VERSION -DprocessAllModules=true
run mvn versions:commit
run git add -u
run git commit -m "Start $NEXT_VERSION development"
run git push origin main

echo ""
if [ "$DRY_RUN" = true ]; then
  echo "✅ Dry-run complete — no changes were made."
  echo "Use './release.sh --execute' to perform the actual release."
else
  echo "🎉 Release v$RELEASE_VERSION complete!"
  echo "🚀 GitHub Actions will now build and publish the release automatically."
fi