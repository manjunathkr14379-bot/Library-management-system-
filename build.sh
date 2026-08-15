#!/usr/bin/env bash
# Build, test, and run the Library Management System without Maven/Gradle.
# Requires: JDK 17+, MySQL Connector/J jar on the classpath for `run`.
#
# Usage:
#   ./build.sh compile   # compile main sources
#   ./build.sh test      # compile + run unit tests (needs junit jars, see README)
#   ./build.sh run       # compile + launch the console app (needs mysql-connector-j)

set -e
SRC_MAIN="src/main/java"
SRC_TEST="src/test/java"
RES="src/main/resources"
OUT_MAIN="out/main"
OUT_TEST="out/test"

# Path to the JUnit5 console-standalone jar (installed via apt: `apt-get install junit5`
# or downloaded manually). Adjust if your system puts it elsewhere.
JUNIT_JAR="${JUNIT_JAR:-/usr/share/java/junit-platform-console-standalone.jar}"

# Path to the MySQL Connector/J jar. Download from
# https://dev.mysql.com/downloads/connector/j/ and point this at the file.
MYSQL_JAR="${MYSQL_JAR:-mysql-connector-j.jar}"

compile() {
    mkdir -p "$OUT_MAIN"
    javac -d "$OUT_MAIN" $(find "$SRC_MAIN" -name "*.java")
    cp -r "$RES"/* "$OUT_MAIN"/
    echo "Compiled main sources to $OUT_MAIN"
}

test() {
    compile
    mkdir -p "$OUT_TEST"
    javac -cp "$JUNIT_JAR:$OUT_MAIN" -d "$OUT_TEST" $(find "$SRC_TEST" -name "*.java")
    java -jar "$JUNIT_JAR" execute --class-path "$OUT_MAIN:$OUT_TEST" --scan-class-path
}

run() {
    compile
    if [ ! -f "$MYSQL_JAR" ]; then
        echo "MySQL Connector/J jar not found at $MYSQL_JAR"
        echo "Download it and set MYSQL_JAR=/path/to/mysql-connector-j-<ver>.jar"
        exit 1
    fi
    java -cp "$OUT_MAIN:$MYSQL_JAR" com.library.ui.ConsoleApp
}

case "$1" in
    compile) compile ;;
    test) test ;;
    run) run ;;
    *) echo "Usage: $0 {compile|test|run}" ;;
esac
