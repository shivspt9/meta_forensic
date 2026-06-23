#!/usr/bin/env bash
# Bootstrap script to download the Maven wrapper jar and run it on POSIX systems
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_DIR="$REPO_ROOT/.mvn/wrapper"
mkdir -p "$WRAPPER_DIR"
JAR_PATH="$WRAPPER_DIR/maven-wrapper.jar"
if [ ! -f "$JAR_PATH" ]; then
  echo "Downloading maven-wrapper.jar..."
  curl -fsSL -o "$JAR_PATH" "https://repo1.maven.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"
fi
java -jar "$JAR_PATH" "$@"
