#!/usr/bin/env bash
# Menjalankan tes logika murni (commonTest/core) tanpa Gradle. Butuh kotlinc (KOTLIN_HOME) + JDK.
#   KOTLIN_HOME=/path/ke/kotlinc tools/logic-tests/run.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
KH="${KOTLIN_HOME:-/tmp/kt/kotlinc}"
OUT="$(mktemp -d)"
CP="$KH/lib/kotlinx-coroutines-core-jvm.jar:$KH/lib/kotlin-stdlib.jar"
CORE="${CORE_DIR:-$ROOT/shared/src/commonMain/kotlin/id/biojelan/app/core}"
"$KH/bin/kotlinc" -nowarn -Xallow-kotlin-package -cp "$CP" -d "$OUT" \
  "$ROOT/tools/logic-tests/shim/KotlinTestShim.kt" \
  "$CORE/AutoRefreshGate.kt" "$CORE/DedupeWindow.kt" "$CORE/ChangeDetection.kt" \
  "$ROOT"/shared/src/commonTest/kotlin/id/biojelan/app/core/*.kt \
  "$ROOT/tools/logic-tests/Runner.kt" 2>&1 | grep -v "^warning" || true
java -cp "$OUT:$CP" RunnerKt "$OUT"
