#!/usr/bin/env bash
###############################################################################
# cleanup-tags.sh
#
# PURPOSE:
#   Safely delete all Git tags both locally and remotely (on GitHub).
#
# USAGE:
#   chmod +x shell-scripts/cleanup-tags.sh
#   ./shell-scripts/cleanup-tags.sh
#
# NOTES:
#   - Removes all tags named refs/tags/*
#   - Filters out annotated tag dereferences (^{} lines)
#   - Prints summary of deletions
###############################################################################

set -euo pipefail

say() {
  printf "\033[1;32m==>\033[0m %s\n" "$*"
}

warn() {
  printf "\033[1;33mWARN:\033[0m %s\n" "$*\n"
}

# Ensure we're in a Git repo
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  warn "Not inside a Git repository."
  exit 1
fi

REMOTE="${1:-origin}"

say "Cleaning up all Git tags (local + remote: $REMOTE)..."

# --- Step 1: Delete all local tags ------------------------------------------
LOCAL_TAGS=$(git tag -l || true)
if [[ -n "$LOCAL_TAGS" ]]; then
  say "Deleting local tags..."
  git tag -l | xargs git tag -d
else
  say "No local tags to delete."
fi

# --- Step 2: Delete all remote tags -----------------------------------------
REMOTE_TAGS=$(git ls-remote --tags "$REMOTE" | awk '/refs\/tags/ && !/\^{}$/{print $2}' || true)

if [[ -n "$REMOTE_TAGS" ]]; then
  say "Deleting remote tags from '$REMOTE'..."
  git ls-remote --tags "$REMOTE" \
    | awk '/refs\/tags/ && !/\^{}$/{print ":" $2}' \
    | xargs git push "$REMOTE"
else
  say "No remote tags found on '$REMOTE'."
fi

say "Fetching to confirm cleanup..."
git fetch --tags --prune

say "✅ All tags have been deleted locally and remotely."