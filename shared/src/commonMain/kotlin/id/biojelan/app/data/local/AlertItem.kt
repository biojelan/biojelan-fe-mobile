package id.biojelan.app.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Satu entri di kotak masuk notifikasi. [at] = epoch millis saat kejadian ditemukan app. */
@Serializable
data class AlertItem(
    val id: Long,
    val text: String,
    val at: Long,
    val read: Boolean = false,
)

/** Batas riwayat yang disimpan: item terlama dibuang saat melewati angka ini. */
const val MAX_ALERT_HISTORY = 30

/** Daftar baru dengan entri [text] di paling depan (terbaru dulu), dipangkas ke [max]. */
fun List<AlertItem>.withNewAlert(text: String, now: Long, max: Int = MAX_ALERT_HISTORY): List<AlertItem> {
    val nextId = (maxOfOrNull { it.id } ?: 0L) + 1L
    return (listOf(AlertItem(id = nextId, text = text, at = now)) + this).take(max)
}

fun List<AlertItem>.allMarkedRead(): List<AlertItem> =
    if (none { !it.read }) this else map { if (it.read) it else it.copy(read = true) }

val List<AlertItem>.unreadCount: Int get() = count { !it.read }

private val alertJson = Json { ignoreUnknownKeys = true }

fun encodeAlerts(items: List<AlertItem>): String =
    alertJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(AlertItem.serializer()), items)

/** Data rusak atau kosong dianggap tidak ada riwayat, bukan alasan app crash saat start. */
fun decodeAlerts(raw: String?, max: Int = MAX_ALERT_HISTORY): List<AlertItem> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        alertJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(AlertItem.serializer()), raw).take(max)
    } catch (e: Exception) {
        emptyList()
    }
}
