#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_REPO="${BUILD_REPO:-$PROJECT_DIR/target/submodule-m2repo}"
PL_SERVER_JAR="${PL_SERVER_JAR:-$PROJECT_DIR/pl_server/pl_server.jar}"
CMT_MODULES="${CMT_MODULES:-plugins/com.cubrid.common.log,plugins/com.cubrid.common.configuration,plugins/com.cubrid.cubridmigration.core}"

if [ "$#" -eq 0 ]; then
    set -- -DskipTests package
fi

echo "Using Maven local repository: $BUILD_REPO"

echo "Installing CMT artifacts from submodule..."
mvn \
    -Dmaven.repo.local="$BUILD_REPO" \
    -f "$PROJECT_DIR/submodule/cubrid-migration/pom.xml" \
    -Pconsole \
    -pl "$CMT_MODULES" \
    -am \
    -DskipTests \
    install

if [ ! -f "$PL_SERVER_JAR" ]; then
    echo "pl_server jar is missing: $PL_SERVER_JAR" >&2
    echo "Place a custom-built pl_server.jar there or set PL_SERVER_JAR=/path/to/pl_server.jar." >&2
    exit 1
fi

echo "Building SQLAnalyzer..."
mvn \
    -Dmaven.repo.local="$BUILD_REPO" \
    -Dpl.server.jar="$PL_SERVER_JAR" \
    -f "$PROJECT_DIR/pom.xml" \
    "$@"
