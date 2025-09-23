#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Move to the project root
PROJECT_ROOT="$SCRIPT_DIR/.."

# Set classpath using the JARs in their respective target folders
CLASSPATH="$PROJECT_ROOT/target/paint-generate-squares-1.0.0-SNAPSHOT-jar-with-dependencies.jar:$PROJECT_ROOT/../paint-shared-utils/target/paint-shared-utils-1.0.0-SNAPSHOT.jar"

# Run the Java class
java -cp "$CLASSPATH" generatesquares.GenerateSquares
