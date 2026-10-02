package id.biojelan.app.core

/**
 * True kalau [after] punya item yang butuh tindakan pengguna ([actionable]) dan belum ada di [before].
 * Dua item dianggap sama kalau [key]-nya sama — sertakan status di key supaya perubahan status
 * (mis. diterima → minta batal) terhitung sebagai kebutuhan tindakan yang baru.
 */
fun <T> hasNewActionable(before: List<T>, after: List<T>, key: (T) -> String, actionable: (T) -> Boolean): Boolean {
    val known = before.filter(actionable).map(key).toSet()
    return after.any { actionable(it) && key(it) !in known }
}

/** Item di [after] yang id-nya sudah ada di [before] tapi statusnya berubah. Item baru/terhapus diabaikan. */
fun <T, S> changedStatus(before: List<T>, after: List<T>, id: (T) -> String, status: (T) -> S): List<T> {
    val previous = before.associate { id(it) to status(it) }
    return after.filter { item -> id(item) in previous && previous[id(item)] != status(item) }
}

/**
 * Apakah penugasan jemput [incomingId] perlu diumumkan sebagai "baru". Hanya kalau muat sebelumnya BERHASIL
 * (kalau gagal, "tidak ada penugasan" di state itu bukan fakta), ada penugasan sungguhan, dan berbeda dari sebelumnya.
 */
fun shouldAnnounceNewAssignment(previousLoadedOk: Boolean, previousId: String?, incomingId: String?): Boolean =
    previousLoadedOk && !incomingId.isNullOrBlank() && incomingId != previousId
