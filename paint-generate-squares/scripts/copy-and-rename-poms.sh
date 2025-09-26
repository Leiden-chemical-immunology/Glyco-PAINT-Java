#!/bin/bash

# Usage: ./copy-and-rename-poms.sh /path/to/modules /path/to/output
MODULES_DIR="/Users/hans/JavaPaintProjects"
OUTPUT_DIR="/Users/hans/Downloads/Pom"

if [ -z "$MODULES_DIR" ] || [ -z "$OUTPUT_DIR" ]; then
    echo "Usage: $0 /path/to/modules /path/to/output"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

# Copy parent pom.xml from root
if [ -f "$MODULES_DIR/pom.xml" ]; then
    cp "$MODULES_DIR/pom.xml" "$OUTPUT_DIR/pom-parent.xml"
    echo "Copied parent POM -> $OUTPUT_DIR/pom-parent.xml"
else
    echo "No parent pom.xml found in $PROJECT_ROOT"
fi

for module_path in "$MODULES_DIR"/*; do
    if [ -d "$module_path" ] && [ -f "$module_path/pom.xml" ]; then
        module_name=$(basename "$module_path")
        cp "$module_path/pom.xml" "$OUTPUT_DIR/pom-$module_name.xml"
        echo "Copied $module_name/pom.xml -> $OUTPUT_DIR/pom-$module_name.xml"
    fi
done