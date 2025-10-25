#!/bin/bash
#
# ================================================================
#  make-glyco-paint-installer.sh
#
#  Description:
#    Creates a cross-platform self-extracting Glyco-PAINT installer that:
#      • Installs Glyco-PAINT into a platform-appropriate Applications folder
#      • Detects Fiji.app automatically in common macOS and Windows locations
#      • Installs the Fiji fat JAR into the Fiji/plugins directory
#      • Outputs to ~/Downloads/Glyco-PAINT-Installer.sh
# ================================================================

set -e

# --- CONFIGURATION ---
MAC_APP_DIR="$HOME/Applications/Glyco-PAINT"
WIN_APP_DIR="$HOME/AppData/Local/Glyco-PAINT"
FIJI_TARGET_DIR="$HOME/JavaPaintProjects/paint-fiji-plugin/target"
OUTPUT_INSTALLER="$HOME/Downloads/Glyco-PAINT-Installer.sh"

echo "📦 Creating Glyco-PAINT self-extracting installer..."

# --- Detect OS ---
OS="$(uname -s)"
if [[ "$OS" == "Darwin" ]]; then
  INSTALL_SOURCE="$MAC_APP_DIR"
else
  INSTALL_SOURCE="$WIN_APP_DIR"
fi

# --- Validate Glyco-PAINT folder ---
if [ ! -d "$INSTALL_SOURCE" ]; then
  echo "❌ Directory not found: $INSTALL_SOURCE"
  exit 1
fi

# --- Find fat JAR automatically ---
FIJI_JAR=$(find "$FIJI_TARGET_DIR" -maxdepth 1 -type f -name '*-jar-with-dependencies.jar' | head -n 1)
if [ -z "$FIJI_JAR" ]; then
  echo "❌ No fat JAR found in: $FIJI_TARGET_DIR"
  exit 1
fi

echo "🧩 Found Fiji JAR: $(basename "$FIJI_JAR")"

# --- Create temporary bundle ---
TMP_DIR=$(mktemp -d)
cp -R "$INSTALL_SOURCE" "$TMP_DIR/Glyco-PAINT"
cp "$FIJI_JAR" "$TMP_DIR/"

# --- Compress payload ---
cd "$TMP_DIR"
tar -czf payload.tar.gz Glyco-PAINT "$(basename "$FIJI_JAR")"
cd - >/dev/null

# --- Write installer script header ---
cat > "$OUTPUT_INSTALLER" <<'EOF'
#!/bin/bash
set -e

echo "🧬 Glyco-PAINT Installer"
echo "========================="

OS="$(uname -s)"
if [[ "$OS" == "Darwin" ]]; then
  INSTALL_APPS="$HOME/Applications/Glyco-PAINT"
else
  INSTALL_APPS="$HOME/AppData/Local/Glyco-PAINT"
fi

TMP_EXTRACT=$(mktemp -d)
cleanup() { rm -rf "$TMP_EXTRACT"; }
trap cleanup EXIT

# Extract embedded payload
PAYLOAD_LINE=$(awk '/^__ARCHIVE_BELOW__/ {print NR + 1; exit 0; }' "$0")
tail -n +$PAYLOAD_LINE "$0" | base64 --decode | tar -xz -C "$TMP_EXTRACT"

echo ""
echo "📂 Installing Glyco-PAINT to:"
echo "   $INSTALL_APPS"
mkdir -p "$(dirname "$INSTALL_APPS")"
rm -rf "$INSTALL_APPS"
cp -R "$TMP_EXTRACT/Glyco-PAINT" "$INSTALL_APPS"

# --------------------------------------------------------
# Cross-platform Fiji.app detection
# --------------------------------------------------------
echo ""
echo "🔍 Looking for Fiji.app ..."

FIJI_APP=""
if [[ "$OS" == "Darwin" ]]; then
  OPTIONS=(
    "/Applications/Fiji.app"
    "$HOME/Applications/Fiji.app"
  )
else
  OPTIONS=(
    "/c/Program Files/Fiji.app"
    "/c/Program Files (x86)/Fiji.app"
    "$HOME/AppData/Local/Fiji.app"
  )
fi

FOUND_PATH=""
for opt in "${OPTIONS[@]}"; do
  if [ -d "$opt" ]; then
    FOUND_PATH="$opt"
    break
  fi
done

if [ -n "$FOUND_PATH" ]; then
  echo "✅ Found Fiji.app at: $FOUND_PATH"
  FIJI_APP="$FOUND_PATH"
else
  echo ""
  echo "Please choose where Fiji.app is installed:"
  i=1
  for opt in "${OPTIONS[@]}"; do
    echo "  $i) $opt"
    ((i++))
  done
  echo "  $i) Enter custom path"
  read -rp "Enter your choice [1-$i]: " choice

  if [[ "$choice" -ge 1 && "$choice" -lt "$i" ]]; then
    FIJI_APP="${OPTIONS[$((choice-1))]}"
  else
    read -rp "Enter full path to Fiji.app: " FIJI_APP
  fi

  if [ ! -d "$FIJI_APP" ]; then
    echo "❌ Invalid path: $FIJI_APP"
    exit 1
  fi
fi

echo ""
echo "📦 Using Fiji at: $FIJI_APP"

# Copy plugin JAR into Fiji plugins folder
FIJI_PLUGINS="$FIJI_APP/plugins"
mkdir -p "$FIJI_PLUGINS"

JAR_FILE=$(find "$TMP_EXTRACT" -type f -name '*-jar-with-dependencies.jar' | head -n 1)
if [ -z "$JAR_FILE" ]; then
  echo "❌ Installer payload missing JAR file!"
  exit 1
fi

echo "🔌 Installing $(basename "$JAR_FILE") to $FIJI_PLUGINS ..."
cp "$JAR_FILE" "$FIJI_PLUGINS/"

echo ""
echo "✅ Installation complete!"
echo "   • Glyco-PAINT: $INSTALL_APPS"
echo "   • Fiji plugin: $FIJI_PLUGINS"
echo ""
echo "🎨 You can now launch Glyco-PAINT from your Applications folder."
exit 0

__ARCHIVE_BELOW__
EOF

# --- Append base64 payload (safe for macOS/Windows) ---
base64 < "$TMP_DIR/payload.tar.gz" >> "$OUTPUT_INSTALLER"

chmod +x "$OUTPUT_INSTALLER"
rm -rf "$TMP_DIR"

echo ""
echo "✅ Created cross-platform self-extracting installer:"
echo "   $OUTPUT_INSTALLER"
echo ""
echo "💡 To install, run:"
echo "   bash Glyco-PAINT-Installer.sh"