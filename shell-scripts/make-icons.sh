#!/bin/bash
#
# ================================================================
#  make-icons.sh
#
#  Description:
#    Converts one or more 1024×1024 PNG icons into both:
#      - macOS .icns bundles (via sips + iconutil)
#      - Windows .ico files (via ImageMagick convert)
#    Then copies both into each app's icons folder under
#      ~/JavaPaintProjects/<module>/src/main/resources/icons/
#
#  Usage:
#    ./make-icons.sh paint-viewer.png paint-get-omero.png paint-generate-squares.png paint-create-experiment.png
# ================================================================

set -e

if [ $# -eq 0 ]; then
  echo "Usage: $0 <icon1.png> [<icon2.png> ...]"
  exit 1
fi

PROJECT_ROOT="$HOME/JavaPaintProjects"
DEST_SUBDIR="src/main/resources/icons"

# Check for ImageMagick
if command -v convert >/dev/null 2>&1; then
  HAVE_IMAGEMAGICK=true
else
  HAVE_IMAGEMAGICK=false
  echo "⚠️  ImageMagick not found. .ico files will not be generated."
  echo "   → Install with: brew install imagemagick"
fi

for pngfile in "$@"; do
  if [ ! -f "$pngfile" ]; then
    echo "❌ File not found: $pngfile"
    continue
  fi

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