#!/usr/bin/env bash
set -euo pipefail

#=============================================================================
# 🚀 release-manager.sh (triggers GitHub Actions + supports delete)
#=============================================================================

export GIT_EDITOR=true
export EDITOR=true
export VISUAL=true

MAVEN_JAVA_HOME="$({ /usr/libexec/java_home -v 21 2>/dev/null || true; })"
[ -n "$MAVEN_JAVA_HOME" ] && export JAVA_HOME="$MAVEN_JAVA_HOME"

export MAVEN_OPTS="${MAVEN_OPTS:-} --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED"

MVN_CMD="mvn"
MAIN_BRANCH="main"
DRY_RUN=true
SHOW_DIFF=true

cd "$(dirname "$0")/.." || exit 1

say() { printf "%s\n" "$*"; }
warn() { printf "⚠️  %s\n" "$*" >&2; }
die() { printf "❌ %s\n" "$*" >&2; exit 1; }

run() {
  echo "👉 $*"
  if [ "$DRY_RUN" = false ]; then "$@"; fi
}

commit_if_needed() {
  local msg="$1"
  if [ -n "$(git status --porcelain)" ]; then
    run git add -u
    run git commit -m "$msg" --no-edit --quiet
  fi
}

require_clean_worktree() {
  if [ -n "$(git status --porcelain)" ]; then
    die "Working tree not clean. Commit or stash changes first."
  fi
}

ensure_on_main() {
  local cur
  cur=$(git rev-parse --abbrev-ref HEAD)
  if [ "$cur" != "$MAIN_BRANCH" ]; then
    warn "Switching to '$MAIN_BRANCH'..."
    run git switch "$MAIN_BRANCH"
  fi
}

confirm() {
  local msg="$1"
  read -rp "$msg (y/n): " ans
  [ "$ans" = "y" ]
}

get_current_version() {
  $MVN_CMD help:evaluate -Dexpression=project.version -q -DforceStdout
}

next_snapshot() {
  local v="${1%%-*}" major minor patch
  IFS='.' read -r major minor patch <<< "$v"
  echo "${major}.${minor}.$((patch+1))-SNAPSHOT"
}

usage() {
  cat <<EOF
Usage:
  $0 create [--execute] [VERSION]
  $0 delete [TAG] [--execute]

Subcommands:
  create     Create and tag a new release (triggers GitHub Actions)
  delete     Delete a Git tag and GitHub release

Options:
  --execute, -x   Run for real (not dry-run)
  --help, -h      Show help
EOF
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

# --- create ---
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

  say "🧪 Verifying build..."
  run $MVN_CMD clean verify -DskipTests

  say "🏷️  Setting release version..."
  run $MVN_CMD versions:set -DnewVersion="$release" -DprocessAllModules=true
  run $MVN_CMD versions:commit

  local CHANGELOG="CHANGELOG.md"
  local DATE HEADER
  DATE=$(date +"%Y-%m-%d")
  HEADER="## v${release} - ${DATE}"

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
    echo "🟡 Diff preview:"
    git diff --color || true
    echo ""
  fi

  say "📝 Commit and tag release..."
  run git add "$CHANGELOG"
  commit_if_needed "Release v${release}"
  run git tag -a "v${release}" -m "Release v${release}"

  say "🚀 Pushing release tag (triggers GitHub Actions)..."
  run git push origin "$MAIN_BRANCH"
  run git push origin "v${release}"

  say "🔄 Bumping to next snapshot..."
  run $MVN_CMD versions:set -DnewVersion="$next" -DprocessAllModules=true
  run $MVN_CMD versions:commit
  commit_if_needed "Start $next development"
  run git push origin "$MAIN_BRANCH"

  if [ "$DRY_RUN" = true ]; then
    say "✅ Dry-run complete — nothing changed."
  else
    say "🎉 Release v${release} created and pushed!"
    say "   → GitHub Actions will build and publish automatically."
  fi
}

# --- delete ---
cmd_delete() {
  local tag="${1:-}"
  if [ -z "$tag" ]; then die "Usage: $0 delete <tag> [--execute]"; fi

  local tagname="v${tag#v}"  # ensure it has v-prefix
  say "🗑️  Deleting release '$tagname'..."

  confirm "Are you sure you want to delete $tagname?" || { say "Aborted."; return 0; }

  if command -v gh >/dev/null 2>&1; then
    say "📢 Checking GitHub for release '$tagname'..."
    if gh release view "$tagname" &>/dev/null; then
      run gh release delete "$tagname" --yes
      say "✅ GitHub release deleted."
    else
      warn "No GitHub release found for '$tagname'."
    fi
  else
    warn "'gh' CLI not found — skipping GitHub release deletion."
  fi

  say "🧹 Deleting Git tag locally and remotely..."
  run git tag -d "$tagname" || warn "Local tag not found."
  run git push --delete origin "$tagname" || warn "Remote tag not found."

  say "✅ Delete complete."
}

# --- dispatch ---
case "$subcommand" in
  create)  cmd_create "${1:-}" ;;
  delete)  cmd_delete "${1:-}" ;;
  rollback) warn "Rollback not implemented yet."; ;;
  ""|-h|--help|help) usage ;;
  *) die "Unknown subcommand: $subcommand" ;;
esac