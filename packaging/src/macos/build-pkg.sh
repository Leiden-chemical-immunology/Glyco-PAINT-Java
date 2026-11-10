#!/bin/bash
#
# Builds the Glyco-PAINT installer for macOS (.pkg) with a choice of components, from the app
# images jpackage made. Called by packaging/pom.xml; uses Apple's pkgbuild and productbuild.
#
#   build-pkg.sh <macos-dir> <plugin-jar> <version> <out-pkg>
#
#   macos-dir   folder with Glyco-PAINT.app, Viewer.app, Generate Squares.app,
#               Create Experiment.app and Get Omero.app (each with its own Java)
#   plugin-jar  the Fiji plugin JAR
#   version     the project version, e.g. 0.0.139-SNAPSHOT
#   out-pkg     the installer to write
#
# Choices (Customize, shown straight away): Glyco-PAINT (on), the four separate apps (off) and the
# Fiji plugin (on). The apps go into /Applications/Glyco-PAINT; the plugin is copied into Fiji by
# fiji-plugin/postinstall.

set -euo pipefail

macos="$1"
plugin="$2"
version="$3"
out="$4"

here=$(cd "$(dirname "$0")" && pwd)
work="$macos/pkg-work"
id_prefix="com.github.jjabakker.glycopaint"
install_dir="/Applications/Glyco-PAINT"
plugin_dir="/Library/Application Support/Glyco-PAINT"

rm -rf "$work"
mkdir -p "$work" "$(dirname "$out")"

# One component package per app. Not relocatable: otherwise the Installer may "update" a copy
# of the app with the same bundle identifier elsewhere instead of installing into install_dir.
component() {
    local name="$1" id="$2"
    local root="$work/root-$id"
    mkdir -p "$root"
    ditto "$macos/$name.app" "$root/$name.app"
    cat > "$work/$id.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<array>
    <dict>
        <key>RootRelativeBundlePath</key>      <string>$name.app</string>
        <key>BundleIsRelocatable</key>         <false/>
        <key>BundleIsVersionChecked</key>      <false/>
        <key>BundleHasStrictIdentifier</key>   <true/>
        <key>BundleOverwriteAction</key>       <string>upgrade</string>
    </dict>
</array>
</plist>
EOF
    pkgbuild --quiet --root "$root" --component-plist "$work/$id.plist" \
             --identifier "$id_prefix.$id" --version "$version" \
             --install-location "$install_dir" "$work/$id.pkg"
}

component "Glyco-PAINT"       "glyco-paint"
component "Viewer"            "viewer"
component "Generate Squares"  "generate-squares"
component "Create Experiment" "create-experiment"
component "Get Omero"         "get-omero"

# The Fiji plugin: installed as a file in plugin_dir (so the Installer shows its size, and it is
# there to copy by hand if no Fiji is found); its script then copies it into Fiji
mkdir -p "$work/plugin-root" "$work/plugin-scripts"
cp "$plugin" "$work/plugin-root/"
cp "$here/fiji-plugin/postinstall" "$work/plugin-scripts/postinstall"
chmod 755 "$work/plugin-scripts/postinstall"
pkgbuild --quiet --root "$work/plugin-root" --scripts "$work/plugin-scripts" \
         --identifier "$id_prefix.fiji-plugin" --version "$version" \
         --install-location "$plugin_dir" "$work/fiji-plugin.pkg"

# The installer with its choices
cat > "$work/distribution.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<installer-gui-script minSpecVersion="2">
    <title>Glyco-PAINT $version</title>
    <options customize="always" require-scripts="false" hostArchitectures="arm64"/>
    <domains enable_localSystem="true"/>

    <choices-outline>
        <line choice="glyco-paint"/>
        <line choice="viewer"/>
        <line choice="generate-squares"/>
        <line choice="create-experiment"/>
        <line choice="get-omero"/>
        <line choice="fiji-plugin"/>
    </choices-outline>

    <choice id="glyco-paint" title="Glyco-PAINT" start_selected="true"
            description="The Glyco-PAINT Launcher, from which all apps can be started. Installed in $install_dir.">
        <pkg-ref id="$id_prefix.glyco-paint"/>
    </choice>
    <choice id="viewer" title="Viewer" start_selected="false"
            description="The Viewer as a separate app. Installed in $install_dir.">
        <pkg-ref id="$id_prefix.viewer"/>
    </choice>
    <choice id="generate-squares" title="Generate Squares" start_selected="false"
            description="Generate Squares as a separate app. Installed in $install_dir.">
        <pkg-ref id="$id_prefix.generate-squares"/>
    </choice>
    <choice id="create-experiment" title="Create Experiment" start_selected="false"
            description="Create Experiment as a separate app. Installed in $install_dir.">
        <pkg-ref id="$id_prefix.create-experiment"/>
    </choice>
    <choice id="get-omero" title="Get Omero" start_selected="false"
            description="Get Omero as a separate app. Installed in $install_dir.">
        <pkg-ref id="$id_prefix.get-omero"/>
    </choice>
    <choice id="fiji-plugin" title="Fiji plugin" start_selected="true"
            description="The Glyco-PAINT plugin for Fiji (TrackMate), copied into the plugins folder of Fiji in /Applications or your own Applications folder. An older Glyco-PAINT plugin there is replaced. A copy is kept in $plugin_dir.">
        <pkg-ref id="$id_prefix.fiji-plugin"/>
    </choice>

    <pkg-ref id="$id_prefix.glyco-paint"       version="$version">glyco-paint.pkg</pkg-ref>
    <pkg-ref id="$id_prefix.viewer"            version="$version">viewer.pkg</pkg-ref>
    <pkg-ref id="$id_prefix.generate-squares"  version="$version">generate-squares.pkg</pkg-ref>
    <pkg-ref id="$id_prefix.create-experiment" version="$version">create-experiment.pkg</pkg-ref>
    <pkg-ref id="$id_prefix.get-omero"         version="$version">get-omero.pkg</pkg-ref>
    <pkg-ref id="$id_prefix.fiji-plugin"       version="$version">fiji-plugin.pkg</pkg-ref>
</installer-gui-script>
EOF

productbuild --quiet --distribution "$work/distribution.xml" --package-path "$work" "$out"
rm -rf "$work"
echo "✅ $out"
