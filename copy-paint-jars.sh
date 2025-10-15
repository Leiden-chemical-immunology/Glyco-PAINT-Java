#!/bin/bash
#
# copy-paint-jars.sh
#
# Collects all runnable JARs (*-jar-with-dependencies.jar)
# from each PAINT module and copies them into paint-launcher/jars/
#

set -e  # stop on first error

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LAUNCHER_DIR="$ROOT_DIR/paint-launcher"
JARS_DIR="$LAUNCHER_DIR/jars"

# Create jars directory if it doesn't exist
mkdir -p "$JARS_DIR"

echo "🧹 Cleaning old JARs..."
rm -f "$JARS_DIR"/*.jar

echo "📦 Collecting runnable JARs..."

# List of module directories to include
MODULES=(
  "paint-generate-squares"
  "paint-viewer"
  "paint-get-omero"
  "paint-create-experiment"
)

for module in "${MODULES[@]}"; do
  TARGET_DIR="$ROOT_DIR/$module/target"
  if [ -d "$TARGET_DIR" ]; then
    jar_file=$(find "$TARGET_DIR" -maxdepth 1 -type f -name "*-jar-with-dependencies.jar" | head -n 1)
    if [ -n "$jar_file" ]; then
      echo "✅ Copying $(basename "$jar_file") from $module"
      cp "$jar_file" "$JARS_DIR/"
    else
      echo "⚠️  No runnable JAR found for $module"
    fi
  else
    echo "⚠️  Target folder not found for $module"
  fi
done

echo
echo "🎯 All runnable JARs copied to:"
echo "   $JARS_DIR"
echo
ls -1 "$JARS_DIR"