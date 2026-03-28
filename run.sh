#!/usr/bin/env bash
# Run FamilyAssistantApp in a clean JVM — no Maven classloader interference.
# Usage: ./run.sh
# Optional env vars: WEBHOOK_PORT (default 8080), GEMINI_API_KEY

set -e
cd "$(dirname "$0")"

mvn compile -q

CP="target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout 2>/dev/null)"

exec java -cp "$CP" com.family.assistant.FamilyAssistantApp
