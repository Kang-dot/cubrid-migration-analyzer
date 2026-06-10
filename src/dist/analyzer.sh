#!/usr/bin/env bash
set -e

APP_HOME="$(cd "$(dirname "$0")" && pwd)"

if [ -z "${CUBRID:-}" ]; then
    echo "CUBRID environment variable is not set." >&2
    exit 1
fi

export LD_LIBRARY_PATH="${CUBRID}/lib:${LD_LIBRARY_PATH:-}"

cd "$APP_HOME"

ANTLR4_RUNTIME="$APP_HOME/lib/antlr4-runtime-4.13.2.jar"

if [ ! -f "$ANTLR4_RUNTIME" ]; then
    echo "Required ANTLR runtime is missing: $ANTLR4_RUNTIME" >&2
    exit 1
fi

exec java \
    -Djava.library.path="$APP_HOME/jni" \
    -Dsqlanalyzer.plcsql.jar="$APP_HOME/lib/pl_server.jar" \
    -cp "$APP_HOME/analyzer.jar:$ANTLR4_RUNTIME:$APP_HOME/lib/*" \
    com.cubrid.sqlanalyzer.command.AnalyzerConsoleMain \
    "$@"
