#!/bin/bash
set -e

#=============================================================================
# clean-all-paint-jars.sh
#
# PURPOSE:
#   Locate all “paint” JAR files (*paint*.jar) in:
#     • Project modules (~/JavaPaintProjects)
#     • Maven local repository (~/.m2)
#     • Fiji installation (/Applications/Fiji.app)
#     • Glyco-PAINT app bundles (~/Applications/Glyco-PAINT)
#   List them to the user with a preview.
#   Prompt for confirmation.
#   If confirmed, delete all listed files.
#
# USE CASE:
#   Run this script before rebuilding, packaging or deploying
#   to ensure no stale “paint” JARs remain.
#
# ACTIONS PERFORMED:
#   1) Search defined directories for matching JAR files.
#   2) Present the found file list to the user.
#   3) Ask “Do you want to delete all these files? (y/N)”.
#   4) If yes → delete files. If no → abort without deletion.
#
# USAGE:
#   ./shell-scripts/clean-all-paint-jars.sh
#=============================================================================

PROJECTS_DIR="$HOME/JavaPaintProjects"
M2_REPO_DIR="$HOME/.m2"
FIJI_APP_DIR="/Applications/Fiji.app"
GLYCO_APP_DIR="$HOME/Applications/Glyco-PAINT"

echo "🔍 Locating Paint JAR files..."

# Find all matching files
files=()
while IFS= read -r -d '' f; do
  files+=("$f")
done < <(
  find "$PROJECTS_DIR" -type f -iname "*paint*.jar" -print0
  find "$M2_REPO_DIR"   -type f -iname "*paint*.jar" -print0
  [ -d "$FIJI_APP_DIR" ] && find "$FIJI_APP_DIR" -type f -iname "*paint*.jar" -print0
  [ -d "$GLYCO_APP_DIR" ] && find "$GLYCO_APP_DIR" -type f -iname "paint*.jar" -print0
)

if [ ${#files[@]} -eq 0 ]; then
  echo "✅ No Paint JAR files found."
  exit 0
fi

echo ""
echo "📋 Found the following files:"
for f in "${files[@]}"; do
  echo "  $f"
done

echo ""
printf "Do you want to delete all these files? (y/N) "
read answer
case "$answer" in
  [yY]|[yY][eE][sS])
    echo "🗑️  Deleting files..."
    for f in "${files[@]}"; do
      rm -v "$f"
    done
    echo "✅ All selected files deleted."
    ;;
  *)
    echo "❌ Operation cancelled. No files were deleted."
    ;;
esac