#!/bin/bash
#
# ================================================================
#  package-glyco-paint.sh
#
#  Description:
#    Compresses the Glyco-PAINT folder and adds the Fiji fat JAR
#    (auto-detected in the paint-fiji-plugin/target directory).
#    The resulting ZIP is saved to ~/Downloads.
#
#  Usage:
#    ./package-glyco-paint.sh
# ================================================================

set -e

GLYCO_DIR="$HOME/Applications/Glyco-PAINT"
FIJI_TARGET_DIR="$HOME/JavaPaintProjects/paint-fiji-plugin/target"
OUTPUT_ZIP="$HOME/Downloads/Glyco-PAINT-Package.zip"

echo "📦 Packaging Glyco-PAINT..."

# Validate Glyco-PAINT directory
if [ ! -d "$GLYCO_DIR" ]; then
  echo "❌ Directory not found: $GLYCO_DIR"
  exit 1
fi

# Find the fat JAR automatically
FIJI_JAR=$(find "$FIJI_TARGET_DIR" -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' | head -n 1)

if [ -z "$FIJI_JAR" ]; then
  echo "❌ No fat JAR (with-dependencies) found in: $FIJI_TARGET_DIR"
  exit 1
fi

echo "🧩 Found Fiji fat JAR: $(basename "$FIJI_JAR")"

# Work in a temporary folder
TMP_DIR=$(mktemp -d)
cp -R "$GLYCO_DIR" "$TMP_DIR/"
cp "$FIJI_JAR" "$TMP_DIR/Glyco-PAINT/"

# Compress
cd "$TMP_DIR"
zip -r "$OUTPUT_ZIP" "Glyco-PAINT" >/dev/null

# Clean up
cd - >/dev/null
rm -rf "$TMP_DIR"

echo "✅ Package created successfully:"
echo "   $OUTPUT_ZIP"