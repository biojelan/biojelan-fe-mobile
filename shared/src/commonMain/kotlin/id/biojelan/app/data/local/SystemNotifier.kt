package id.biojelan.app.data.local

/**
 * Penampil notifikasi level sistem (bar notifikasi HP). Implementasi per platform; di platform yang belum
 * punya implementasi dipakai [NoopSystemNotifier] sehingga hanya toast + kotak masuk di dalam app yang aktif.
 */
interface SystemNotifier {
    fun show(text: String)
}

object NoopSystemNotifier : SystemNotifier {
    override fun show(text: String) {}
}
