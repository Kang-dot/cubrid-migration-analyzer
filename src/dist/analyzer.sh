#!/usr/bin/env bash
set -e

APP_HOME="$(cd "$(dirname "$0")" && pwd)"

if [ -z "${CUBRID:-}" ]; then
    echo "CUBRID environment variable is not set." >&2
    exit 1
fi

export LD_LIBRARY_PATH="${CUBRID}/lib:${LD_LIBRARY_PATH:-}"

cd "$APP_HOME"

exec java \
    -Djava.library.path="$APP_HOME/jni" \
    -cp "$APP_HOME/analyzer.jar:$APP_HOME/lib/*" \
    com.cubrid.sqlanalyzer.command.AnalyzerConsoleMain \
    "$@"
