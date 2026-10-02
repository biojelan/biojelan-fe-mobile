// Pengganti minimal API `kotlin.test` HANYA untuk menjalankan tes logika murni lewat kotlinc biasa (tanpa Gradle).
// Tes di commonTest tetap ditulis terhadap `kotlin.test` asli; di Gradle, library aslinya yang dipakai.
package kotlin.test

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test

fun fail(message: String? = null): Nothing = throw AssertionError(message ?: "fail()")

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) fail((message?.let { "$it. " } ?: "") + "Expected <$expected>, actual <$actual>.")
}

fun assertTrue(actual: Boolean, message: String? = null) {
    if (!actual) fail(message ?: "Expected value to be true.")
}

fun assertFalse(actual: Boolean, message: String? = null) {
    if (actual) fail(message ?: "Expected value to be false.")
}

fun assertNull(actual: Any?, message: String? = null) {
    if (actual != null) fail(message ?: "Expected value to be null, but was <$actual>.")
}
