#!/usr/bin/env bash
set -e

###############################################################################
# 🧱 copy-paint-jars.sh
#
# PURPOSE:
#   Collect all runnable “fat JARs” (*-jar-with-dependencies.jar)
#   from each Paint module and copy them into the launcher directory:
#       paint-launcher/jars/
#
# USE CASE:
#   Run this after building all modules (`mvn clean package`)
#   to gather runnable JARs in one place for launching or packaging.
#
# ACTIONS PERFORMED:
#   1️⃣  Removes any old JARs in paint-launcher/jars/
#   2️⃣  Scans selected Paint modules for fat JARs
#   3️⃣  Copies each discovered JAR into the central JAR folder
#
# USAGE:
#   ./shell-scripts/copy-paint-jars.sh
#
# REQUIREMENTS:
#   - Each module must have been built with its `-jar-with-dependencies.jar`
#     (i.e., via `mvn clean package` or `mvn assembly:single`).
#
# RESULT:
#   All runnable JARs end up in:
#       paint-launcher/jars/
#
###############################################################################

# === INITIAL SETUP ===
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LAUNCHER_DIR="$ROOT_DIR/paint-launcher"
JARS_DIR="$LAUNCHER_DIR/jars"

# === PREPARE DESTINATION DIRECTORY ===
mkdir -p "$JARS_DIR"

echo "🧹 Cleaning old JARs..."
rm -f "$JARS_DIR"/*.jar

echo "📦 Collecting runnable JARs..."
echo "Root directory: $ROOT_DIR"
echo ""

###############################################################################
# 🔍 MODULE CONFIGURATION
###############################################################################

MODULES=(
  "paint-generate-squares"
  "paint-viewer"
  "paint-get-omero"
  "paint-create-experiment"
)

###############################################################################
# 🚚 MAIN LOOP: COPY JARS FROM MODULES
###############################################################################

for module in "${MODULES[@]}"; do
  TARGET_DIR="$ROOT_DIR/$module/target"
  echo "🔍 Checking module: $module"
  echo "    → Looking in: $TARGET_DIR"

  if [ -d "$TARGET_DIR" ]; then
    jar_file=$(find "$TARGET_DIR" -maxdepth 1 -type f -name "*-jar-with-dependencies.jar" | head -n 1)

    if [ -n "$jar_file" ]; then
      echo "✅ Found JAR: $(basename "$jar_file")"
      echo "   Copying to: $JARS_DIR"
      cp -v "$jar_file" "$JARS_DIR/"
    else
      echo "⚠️  No runnable JAR found for $module"
      echo "    (Expected: *-jar-with-dependencies.jar)"
    fi
  else
    echo "⚠️  Target folder not found for $module"
  fi

  echo "------------------------------------------------------------"
done

###############################################################################
# ✅ SUMMARY
###############################################################################

echo ""
echo "🎯 All runnable JARs copied to:"
echo "   $JARS_DIR"
echo ""
ls -lh "$JARS_DIR" || echo "⚠️ No JARs found after copy."