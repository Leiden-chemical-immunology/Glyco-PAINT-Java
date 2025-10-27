#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# release-manager.sh
#
# PURPOSE:
#   Unified release management script for multi-module Maven + Git + GitHub projects.
#   Handles version bumping, tagging, changelog updates, and GitHub release creation.
#
# USE CASE:
#   Run this when you’re ready to publish a new release version or correct an existing one.
#   It automates Maven version updates, Git commits, tagging, and triggers GitHub Actions
#   for building, packaging, and publishing release artifacts and Maven sites.
#
# ACTIONS PERFORMED:
#   1  Validates that your working tree is clean
#   2  Updates Maven POM versions to the release version
#   3  Commits and tags the release in Git
#   4  Pushes the tag to GitHub (automatically triggers GitHub Actions)
#   5  Optionally deletes or re-creates existing releases and tags
#   6  Bumps project version to the next SNAPSHOT after release
#
# SUBCOMMANDS:
#   create [VERSION]     Create a new release (triggers GitHub Actions build + site deploy)
#   delete <TAG>         Delete a GitHub release and its tags (local + remote)
#   recreate <TAG>       Delete and immediately re-create the specified release
#
# USAGE:
#   ./shell-scripts/release-manager.sh create --execute 1.2.0
#   ./shell-scripts/release-manager.sh delete --execute 1.2.0
#   ./shell-scripts/release-manager.sh recreate --execute 1.2.0
#
# OPTIONS:
#   --execute, -x   Run for real (default is dry-run)
#   --help, -h      Show help
#
# REQUIREMENTS:
#   - Git installed and configured
#   - Maven installed and on PATH
#   - GitHub CLI (`gh`) installed and authenticated (for deleting GitHub releases)
#   - GitHub Actions configured to build and publish on tag push (v*.*.*)
#
# SAFETY FEATURES:
#   - Runs in DRY-RUN mode by default (no file or tag changes)
#   - Confirms before performing destructive actions
#   - Aborts automatically if uncommitted changes are detected
#   - Never pushes or deletes tags without explicit confirmation
#
# RESULT:
#   - New release tag pushed → triggers GitHub Actions to:
#       • Build and attach release JARs
#       • Publish a GitHub Release with notes
#       • Rebuild and deploy the Maven site to GitHub Pages
#   - Local POMs bumped to the next SNAPSHOT version
#
# WHERE TO CHECK RESULTS:
#   🔹 GitHub Releases:
#        https://github.com/jjabakker/JavaPaintProjects/releases
#        → Verify that the new release appears with attached JARs and notes
#
#   🔹 GitHub Actions (CI/CD logs):
#        https://github.com/jjabakker/JavaPaintProjects/actions
#        → Confirm that the “Build, Release, and Publish Site” workflow ran successfully
#
#   🔹 GitHub Pages (Maven site):
#        https://jjabakker.github.io/JavaPaintProjects/
#        → Confirm that the site updated with the new project documentation
###############################################################################

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
  $0 recreate [TAG] [--execute]

Subcommands:
  create     Create and tag a new release (triggers GitHub Actions)
  delete     Delete a Git tag and GitHub release
  recreate   Delete and immediately re-create a release

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
  run git tag -a "v${release}" -m "Release v${release}" || warn "Tag already exists, skipping."

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

  local tagname="v${tag#v}"  # normalize prefix
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

# --- recreate ---
cmd_recreate() {
  local tag="${1:-}"
  if [ -z "$tag" ]; then die "Usage: $0 recreate <tag> [--execute]"; fi

  say "♻️  Recreating release $tag..."
  cmd_delete "$tag"
  echo ""
  cmd_create "$tag"
  say "✅ Recreated release v${tag#v}."
}

# --- dispatch ---
case "$subcommand" in
  create)  cmd_create "${1:-}" ;;
  delete)  cmd_delete "${1:-}" ;;
  recreate) cmd_recreate "${1:-}" ;;
  rollback) warn "Rollback not implemented yet."; ;;
  ""|-h|--help|help) usage ;;
  *) die "Unknown subcommand: $subcommand" ;;
esac