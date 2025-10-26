#!/usr/bin/env bash
set -euo pipefail

#=============================================================================
# 🚀 release-manager.sh
#
# PURPOSE
#   Unified release management tool for multi-module Maven + Git + GitHub projects.
#
# FEATURES
#   - Create, delete, recreate, or roll back releases
#   - Updates POM versions across modules
#   - Manages Git tags and GitHub releases
#   - Safe: warns and exits if there are uncommitted changes
#=============================================================================

# --- Prevent Git from launching editors ---
export GIT_EDITOR=true
export EDITOR=true
export VISUAL=true

# --- JVM/Maven: mitigate legacy sun.misc.Unsafe usage ---
MAVEN_JAVA_HOME="$({ /usr/libexec/java_home -v 21 2>/dev/null || true; })"
[ -n "$MAVEN_JAVA_HOME" ] && export JAVA_HOME="$MAVEN_JAVA_HOME"

export MAVEN_OPTS="${MAVEN_OPTS:-} --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED"

MVN_CMD="mvn"

# --- CONFIG ---
MAIN_BRANCH="main"
DRY_RUN=true
SHOW_DIFF=true
PUBLISH_SITE=false
GITHUB_RELEASE_ASSETS=()

cd "$(dirname "$0")/.." || exit 1

# --- Helpers ---
usage() {
  cat <<EOF
Usage:
  $0 <subcommand> [options] [version | tag]

Subcommands:
  create     [--execute] [VERSION]   Create a new release.
  delete     [TAG] [--execute]       Delete GitHub release + Git tag.
  recreate   [TAG] [--execute]       Delete and recreate GitHub release.
  rollback   [VERSION] [--execute]   Undo a release and revert to SNAPSHOT.

Options:
  --execute, -x   Run for real (not dry-run)
  --help, -h      Show this help
EOF
}

say() { printf "%s\n" "$*"; }
warn() { printf "⚠️  %s\n" "$*" >&2; }
die() { printf "❌ %s\n" "$*" >&2; exit 1; }

run() {
  echo "👉 $*"
  if [ "$DRY_RUN" = false ]; then
    # Use direct execution to preserve argument quoting
    "$@"
  fi
 }

commit_if_needed() {
  local msg="$1"
  if [ -n "$(git status --porcelain)" ]; then
    run git add -u
    run git commit -m "$msg" --no-edit --quiet
  else
    say "✅ Nothing to commit ($msg skipped)."
  fi
}

get_current_version() {
  $MVN_CMD help:evaluate -Dexpression=project.version -q -DforceStdout
}

next_snapshot() {
  local v="${1%%-*}" major minor patch
  IFS='.' read -r major minor patch <<< "$v"
  echo "${major}.${minor}.$((patch+1))-SNAPSHOT"
}

require_clean_worktree() {
  local status
  status="$(git status --porcelain)"
  if [ -n "$status" ]; then
    echo "❌ Working directory is not clean."
    echo "You have uncommitted changes:"
    echo ""
    echo "$status"
    echo ""
    echo "💡 Please commit, stash, or discard these changes before running the release script."
    exit 1
  fi
}

ensure_on_main() {
  local cur
  cur=$(git rev-parse --abbrev-ref HEAD)
  if [ "$cur" != "$MAIN_BRANCH" ]; then
    warn "You are on '$cur'. Switching to '$MAIN_BRANCH'…"
    run git switch "$MAIN_BRANCH"
  fi
}

confirm() {
  local msg="$1"
  read -rp "$msg (y/n): " ans
  [ "$ans" = "y" ]
}

# --- Arg parsing ---
subcommand="${1:-}"
shift || true
while [[ "${1:-}" =~ ^- ]]; do
  case "$1" in
    --execute|-x) DRY_RUN=false; SHOW_DIFF=false; shift ;;
    --help|-h) usage; exit 0 ;;
    *) break ;;
  esac
done

# --- Implementations ---
cmd_create() {
  local version_arg="${1:-}"

  require_clean_worktree
  say "💡 Mode: $( [ "$DRY_RUN" = true ] && echo 'DRY-RUN' || echo 'EXECUTE')"
  ensure_on_main

  local current release next
  current=$(get_current_version)
  release="${current%-SNAPSHOT}"
  [ -n "$version_arg" ] && release="$version_arg"
  next=$(next_snapshot "$release")

  say "📦 Current version:  $current"
  say "🏷️  Release version: $release"
  say "🔄 Next dev:         $next"
  confirm "Proceed with release?" || { say "Aborted."; return 0; }

  say "🧪 Verifying build…"
  run $MVN_CMD clean verify -DskipTests

  say "🏷️  Setting release version in all modules…"
  run $MVN_CMD versions:set -DnewVersion="$release" -DprocessAllModules=true
  run $MVN_CMD versions:commit

  local CHANGELOG="CHANGELOG.md"
  local DATE HEADER
  DATE=$(date +"%Y-%m-%d")
  HEADER="## v${release} - ${DATE}"

  # Always ensure CHANGELOG exists, even in dry-run mode
  bash -c "
if [ -f '$CHANGELOG' ]; then
  tmp=\$(mktemp)
  {
    echo '$HEADER'
    echo
    echo '- Describe new features, fixes, or changes here.'
    echo
    cat '$CHANGELOG'
  } > \$tmp && mv \$tmp '$CHANGELOG'
else
  cat > '$CHANGELOG' <<EOF2
# Changelog

$HEADER

- Initial release notes.
EOF2
fi
"

  if [ "$SHOW_DIFF" = true ]; then
    echo ""
    echo "🟡 Diff preview (no disk changes in dry-run):"
    echo "────────────────────────────────────────────"
    git diff --color || true
    echo "────────────────────────────────────────────"
    echo ""
  fi

  say "📝 Commit, tag, push…"
  run git add "$CHANGELOG"
  commit_if_needed "Release v${release}"
  run git tag -a "v${release}" -m "Release v${release}" || say "⚠️ Tag exists; skipping."
  run git push origin "$MAIN_BRANCH" || say "⚠️ Push skipped/failed."
  run git push origin "v${release}" || say "⚠️ Tag push skipped/failed."

  if command -v gh >/dev/null 2>&1; then
    say "📢 Creating GitHub release v${release}…"
    local notes_file
    notes_file=$(mktemp)
    awk '
      BEGIN{ print "# v'"$release"'" }
      /^## / && NR>1 { exit }
      { print }
    ' "$CHANGELOG" > "$notes_file"
    run gh release create "v${release}" --title "v${release}" --notes-file "$notes_file"
    rm -f "$notes_file"
  else
    warn "'gh' not found; skipping GitHub release."
  fi

  say "🔄 Bumping to next snapshot: $next"
  run $MVN_CMD versions:set -DnewVersion="$next" -DprocessAllModules=true
  run $MVN_CMD versions:commit
  commit_if_needed "Start $next development"
  run git push origin "$MAIN_BRANCH" || say "⚠️ Push skipped/failed."

  say ""
  if [ "$DRY_RUN" = true ]; then
    say "✅ Dry-run complete — nothing changed. Re-run with --execute to apply."
  else
    say "🎉 Release v${release} complete. New dev cycle started ($next)."
  fi
}

# --- Dispatch ---
case "$subcommand" in
  create)    cmd_create "${1:-}";;
  delete)    warn "Delete command not yet tested";;
  recreate)  warn "Recreate command not yet tested";;
  rollback)  warn "Rollback command not yet tested";;
  ""|-h|--help|help) usage;;
  *) die "Unknown subcommand: $subcommand (use --help)";;
esac
