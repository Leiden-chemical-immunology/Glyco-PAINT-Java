#!/bin/bash
#
###############################################################################
# make-icons.sh
#
# PURPOSE:
#   Unified icon generation script for all Glyco-PAINT desktop applications.
#   Converts high-resolution PNG source images into platform-specific icons
#   (.icns for macOS, .ico for Windows) and installs them into each module’s
#   resource directory for packaging.
#
# USE CASE:
#   Run this script whenever PNG icon artwork changes. It rebuilds all required
#   app icons for macOS and Windows automatically and places them in the proper
#   module resource locations.
#
# ACTIONS PERFORMED:
#   1  Scans the source directory:
#        ~/JavaPaintProjects/paint-icons-generation/
#   2  For each 1024×1024 PNG file found:
#        • Creates a macOS .iconset directory
#        • Uses `sips` and `iconutil` to generate a .icns bundle
#        • Optionally uses ImageMagick `convert` to generate a .ico file
#   3  Copies both icon types into each module’s resource directory:
#        ~/JavaPaintProjects/<module>/src/main/resources/icons/
#
# SUPPORTED MODULES:
#   • paint-generate-squares
#   • paint-create-experiment
#   • paint-get-omero
#   • paint-viewer
#
# REQUIREMENTS:
#   - macOS system (for `sips` and `iconutil`)
#   - ImageMagick (optional, for .ico generation)
#       → install via: brew install imagemagick
#
# USAGE:
#   chmod +x make-icons.sh
#   ./make-icons.sh
#
# RESULT:
#   Each module’s `src/main/resources/icons/` directory will contain:
#     • paint-<app>.icns  — used for macOS .app bundles
#     • paint-<app>.ico   — used for Windows .exe packaging (if ImageMagick found)
#
# SAFETY FEATURES:
#   - Exits early if source directory not found
#   - Skips copy for unknown base names
#   - Gracefully handles missing ImageMagick dependency
#
# WHERE TO CHECK RESULTS:
#   🔹 macOS icon bundles:
#        ~/JavaPaintProjects/<module>/src/main/resources/icons/*.icns
#   🔹 Windows icon files:
#        ~/JavaPaintProjects/<module>/src/main/resources/icons/*.ico
#   🔹 PNG source files:
#        ~/JavaPaintProjects/paint-icons-generation/
#
###############################################################################

set -e

PROJECT_ROOT="$HOME/JavaPaintProjects"
ICON_SOURCE_DIR="$PROJECT_ROOT/paint-icons-generation"
DEST_SUBDIR="src/main/resources/icons"

# Ensure the source directory exists
if [ ! -d "$ICON_SOURCE_DIR" ]; then
  echo "❌ Source directory not found: $ICON_SOURCE_DIR"
  exit 1
fi

cd "$ICON_SOURCE_DIR"

png_files=( *.png )

if [ ${#png_files[@]} -eq 0 ]; then
  echo "⚠️  No PNG files found in $ICON_SOURCE_DIR"
  exit 0
fi

# Check for ImageMagick
if command -v convert >/dev/null 2>&1; then
  HAVE_IMAGEMAGICK=true
else
  HAVE_IMAGEMAGICK=false
  echo "⚠️  ImageMagick not found. .ico files will not be generated."
  echo "   → Install with: brew install imagemagick"
fi

for pngfile in "${png_files[@]}"; do
  base="$(basename "$pngfile" .png)"
  iconset="${base}.iconset"

  echo "🎨 Processing: $base"

  rm -rf "$iconset"
  mkdir "$iconset"

  # macOS iconset generation
  sips -z 16 16     "$pngfile" --out "$iconset/icon_16x16.png" >/dev/null
  sips -z 32 32     "$pngfile" --out "$iconset/icon_16x16@2x.png" >/dev/null
  sips -z 32 32     "$pngfile" --out "$iconset/icon_32x32.png" >/dev/null
  sips -z 64 64     "$pngfile" --out "$iconset/icon_32x32@2x.png" >/dev/null
  sips -z 128 128   "$pngfile" --out "$iconset/icon_128x128.png" >/dev/null
  sips -z 256 256   "$pngfile" --out "$iconset/icon_128x128@2x.png" >/dev/null
  sips -z 256 256   "$pngfile" --out "$iconset/icon_256x256.png" >/dev/null
  sips -z 512 512   "$pngfile" --out "$iconset/icon_256x256@2x.png" >/dev/null
  sips -z 512 512   "$pngfile" --out "$iconset/icon_512x512.png" >/dev/null
  cp "$pngfile" "$iconset/icon_512x512@2x.png"

  # macOS .icns
  iconutil -c icns "$iconset" >/dev/null
  echo "✅ Created: ${base}.icns"

  # Windows .ico
  if [ "$HAVE_IMAGEMAGICK" = true ]; then
    convert "$pngfile" -define icon:auto-resize=256,128,64,48,32,16 "${base}.ico"
    echo "🪟 Created: ${base}.ico"
  fi

  # Determine destination based on app name
  case "$base" in
    paint-viewer)
      dest_dir="$PROJECT_ROOT/paint-viewer/$DEST_SUBDIR"
      ;;
    paint-get-omero)
      dest_dir="$PROJECT_ROOT/paint-get-omero/$DEST_SUBDIR"
      ;;
    paint-generate-squares)
      dest_dir="$PROJECT_ROOT/paint-generate-squares/$DEST_SUBDIR"
      ;;
    paint-create-experiment)
      dest_dir="$PROJECT_ROOT/paint-create-experiment/$DEST_SUBDIR"
      ;;
    *)
      echo "⚠️  Unknown project for $base — skipping copy"
      continue
      ;;
  esac

  mkdir -p "$dest_dir"

  # Copy both icon formats
  cp "${base}.icns" "$dest_dir/"
  if [ -f "${base}.ico" ]; then
    cp "${base}.ico" "$dest_dir/"
  fi

  echo "📦 Copied icons to: $dest_dir"
done

echo "🎉 All icons generated and copied successfully!"