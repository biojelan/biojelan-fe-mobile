# Tes logika murni (tanpa Gradle)

Tes di `shared/src/commonTest/.../core` ditulis dengan API `kotlin.test` standar. Gradle belum punya konfigurasi
host-test untuk target Android (`androidLibrary {}` AGP 9), jadi folder ini menjalankan tes yang sama langsung lewat
`kotlinc` — dengan pengganti minimal `kotlin.test` (`shim/`) dan runner refleksi (`Runner.kt`).

```bash
KOTLIN_HOME=/path/ke/kotlinc tools/logic-tests/run.sh            # jalankan semua tes
KOTLIN_HOME=/path/ke/kotlinc tools/logic-tests/mutation-check.py # uji mutasi: tiap kerusakan kode harus menggagalkan tes
```

Hanya file murni (stdlib + kotlinx-coroutines) yang dikompilasi: `AutoRefreshGate`, `DedupeWindow`, `ChangeDetection`.
ViewModel/Compose/Ktor tidak ikut karena butuh Gradle + Maven.

Di Gradle: dependency `kotlin-test` sudah ada di `commonTest`. Menjalankannya di Android perlu mengaktifkan host test
pada `androidLibrary {}` (nama DSL-nya berbeda antar versi AGP — cek dokumentasi AGP 9.1), atau jalankan
`./gradlew :shared:iosSimulatorArm64Test` di macOS.
