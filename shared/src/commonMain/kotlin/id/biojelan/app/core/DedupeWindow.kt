package id.biojelan.app.core

/**
 * Menyaring pesan identik berturut-turut: pesan yang sama dalam [windowMs] sejak yang TERAKHIR TAMPIL
 * dibuang (yang dibuang tidak memperpanjang jendela). Satu refresh dengan beberapa request yang gagal
 * karena sebab sama jadi satu toast, bukan tiga.
 */
class DedupeWindow(private val windowMs: Long, private val now: () -> Long) {
    private var lastText: String? = null
    private var lastEmittedAt: Long = 0L

    fun shouldEmit(text: String): Boolean {
        val t = now()
        val elapsed = t - lastEmittedAt
        // elapsed negatif = jam diputar mundur: anggap sudah di luar jendela.
        if (text == lastText && elapsed in 0 until windowMs) return false
        lastText = text
        lastEmittedAt = t
        return true
    }
}
