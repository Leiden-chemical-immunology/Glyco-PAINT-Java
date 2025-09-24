#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Move to the project root
PROJECT_ROOT="$SCRIPT_DIR/.."

cp -v $PROJECT_ROOT/target/paint-generate-squares-1.0.0-SNAPSHOT-jar-with-dependencies.jar ~/Applications//Generate\ Squares.app/Contents/Java/
cp -v $PROJECT_ROOT/../paint-shared-utils/target/paint-shared-utils-1.0.0-SNAPSHOT.jar ~/Applications//Generate\ Squares.app/Contents/Java/
