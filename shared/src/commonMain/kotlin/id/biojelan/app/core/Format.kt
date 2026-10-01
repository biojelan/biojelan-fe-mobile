package id.biojelan.app.core

import kotlin.math.abs
import kotlin.math.roundToLong

private const val MS_PER_DAY = 86_400_000L
private val MONTHS_ID = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

// ---------------------------------------------------------------- angka

private fun groupThousands(value: Long): String {
    val digits = abs(value).toString()
    val sb = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return if (value < 0) "-$sb" else sb.toString()
}

/** 12.5 -> "12,5"; 12.0 -> "12"; 1234.56 (maxDecimals=2) -> "1.234,56". */
fun formatNumber(value: Double, maxDecimals: Int = 1): String {
    var factor = 1L
    repeat(maxDecimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToLong()
    val whole = scaled / factor
    val frac = scaled % factor
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    val fracText = if (frac == 0L) "" else "," + frac.toString().padStart(maxDecimals, '0').trimEnd('0')
    return sign + groupThousands(whole) + fracText
}

/** 6500 -> "Rp 6.500" */
fun formatRupiah(value: Long): String = "Rp " + groupThousands(value)

/** 273000 -> "Rp 273rb"; 1230000 -> "Rp 1,2jt" */
fun formatRupiahCompact(value: Long): String = when {
    value >= 1_000_000L -> "Rp " + formatNumber(value / 1_000_000.0, 1) + "jt"
    value >= 1_000L -> "Rp " + ((value + 500) / 1_000) + "rb"
    else -> "Rp $value"
}

/** 12.0 -> "12 L"; 12.5 -> "12,5 L" */
fun formatLiter(value: Double): String = formatNumber(value, 2) + " L"

// ---------------------------------------------------------------- teks

/** "Siti Rahayu" -> "SR". */
fun initialsOf(name: String, max: Int = 2): String {
    val parts = name.trim().split(' ').filter { it.isNotBlank() }
    if (parts.isEmpty()) return "?"
    return parts.take(max).joinToString("") { it.first().uppercase() }
}

/** "Agen Barokah Jaya" -> "BJ" (awalan "Agen" tidak dihitung). */
fun agenInitials(name: String): String = initialsOf(name.trim().removePrefix("Agen ").removePrefix("agen "))

// ---------------------------------------------------------------- tanggal & waktu

/** Hari sejak 1970-01-01 dari tanggal sipil (algoritma Howard Hinnant). */
internal fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (month + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146_097L + doe - 719_468L
}

/** Kebalikan [daysFromCivil] -> Triple(tahun, bulan, hari). */
internal fun civilFromDays(epochDay: Long): Triple<Int, Int, Int> {
    val z = epochDay + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val doe = (z - era * 146_097L).toInt()
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era.toInt() * 400 + (if (m <= 2) 1 else 0)
    return Triple(y, m, d)
}

private val ISO_REGEX = Regex(
    """^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?)?\s*(Z|[+-]\d{2}(?::?\d{2})?)?$"""
)

/** Parse ISO-8601 ("2024-06-01T12:00:00Z") ke epoch millis. Tanpa zona dianggap UTC. */
fun parseIsoMillis(iso: String?): Long? {
    val text = iso?.trim().orEmpty()
    if (text.isEmpty()) return null
    val m = ISO_REGEX.matchEntire(text) ?: return null
    val g = m.groupValues
    val year = g[1].toInt()
    val month = g[2].toInt()
    val day = g[3].toInt()
    if (month !in 1..12 || day !in 1..31) return null
    val hour = g[4].ifEmpty { "0" }.toInt()
    val minute = g[5].ifEmpty { "0" }.toInt()
    val second = g[6].ifEmpty { "0" }.toInt()
    val millis = if (g[7].isEmpty()) 0 else g[7].padEnd(3, '0').take(3).toInt()

    var result = daysFromCivil(year, month, day) * MS_PER_DAY +
        hour * 3_600_000L + minute * 60_000L + second * 1_000L + millis

    val zone = g[8]
    if (zone.isNotEmpty() && zone != "Z") {
        val sign = if (zone[0] == '-') -1 else 1
        val digits = zone.drop(1).replace(":", "")
        val oh = digits.take(2).toInt()
        val om = if (digits.length >= 4) digits.substring(2, 4).toInt() else 0
        result -= sign * (oh * 3_600_000L + om * 60_000L)
    }
    return result
}

internal data class LocalParts(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val epochDay: Long)

internal fun localParts(epochMillis: Long, offsetSeconds: Int): LocalParts {
    val local = epochMillis + offsetSeconds * 1_000L
    val epochDay = local.floorDiv(MS_PER_DAY)
    val msOfDay = local.mod(MS_PER_DAY)
    val (y, m, d) = civilFromDays(epochDay)
    val hour = (msOfDay / 3_600_000L).toInt()
    val minute = ((msOfDay % 3_600_000L) / 60_000L).toInt()
    return LocalParts(y, m, d, hour, minute, epochDay)
}

private fun two(n: Int) = n.toString().padStart(2, '0')

private fun datePart(p: LocalParts) = "${p.day} ${MONTHS_ID[p.month - 1]} ${p.year}"
private fun timePart(p: LocalParts) = "${two(p.hour)}.${two(p.minute)}"

/** "2024-06-01T12:00:00Z" -> "1 Jun 2024, 19.00" (WIB). Gagal parse -> teks asli. */
fun formatDateTime(iso: String?, offsetSeconds: Int = localUtcOffsetSeconds()): String {
    val ms = parseIsoMillis(iso) ?: return iso.orEmpty()
    val p = localParts(ms, offsetSeconds)
    return datePart(p) + ", " + timePart(p)
}

/** "Hari ini, 10.42" / "Kemarin, 16.20" / "7 Agu 2026, 10.42". */
fun formatRelativeDateTime(
    iso: String?,
    nowMillis: Long = nowEpochMillis(),
    offsetSeconds: Int = localUtcOffsetSeconds(),
): String {
    val ms = parseIsoMillis(iso) ?: return iso.orEmpty()
    val p = localParts(ms, offsetSeconds)
    val today = localParts(nowMillis, offsetSeconds).epochDay
    return when (p.epochDay) {
        today -> "Hari ini, " + timePart(p)
        today - 1 -> "Kemarin, " + timePart(p)
        else -> datePart(p) + ", " + timePart(p)
    }
}

/** "10.42" — jam lokal saja. */
fun formatTimeOnly(iso: String?, offsetSeconds: Int = localUtcOffsetSeconds()): String {
    val ms = parseIsoMillis(iso) ?: return ""
    return timePart(localParts(ms, offsetSeconds))
}

fun isToday(
    iso: String?,
    nowMillis: Long = nowEpochMillis(),
    offsetSeconds: Int = localUtcOffsetSeconds(),
): Boolean {
    val ms = parseIsoMillis(iso) ?: return false
    return localParts(ms, offsetSeconds).epochDay == localParts(nowMillis, offsetSeconds).epochDay
}

// ---------------------------------------------------------------- hari buka

/** ["senin","selasa",...,"minggu"] -> "Setiap hari"; ["senin","selasa","rabu"] -> "Senin–Rabu". */
fun formatOpenDays(days: List<String>): String {
    val order = listOf("senin", "selasa", "rabu", "kamis", "jumat", "sabtu", "minggu")
    val picked = order.filter { d -> days.any { it.trim().equals(d, ignoreCase = true) } }
    if (picked.isEmpty()) return "-"
    if (picked.size == 7) return "Setiap hari"
    fun cap(s: String) = s.replaceFirstChar { it.uppercase() }
    val idx = picked.map { order.indexOf(it) }
    val contiguous = idx.zipWithNext().all { (a, b) -> b == a + 1 }
    return if (contiguous && picked.size >= 3) cap(picked.first()) + "–" + cap(picked.last())
    else picked.joinToString(", ") { cap(it) }
}

/** "08:00" -> "08.00" */
fun formatClock(value: String?): String = value.orEmpty().replace(':', '.')

/** "08.00 – 17.00 WIB, Senin–Sabtu" */
fun formatOperatingHours(openAt: String?, closeAt: String?, days: List<String>): String {
    val hours = if (openAt.isNullOrBlank() || closeAt.isNullOrBlank()) "Jam belum diatur"
    else "${formatClock(openAt)} – ${formatClock(closeAt)} WIB"
    val d = formatOpenDays(days)
    return if (d == "-") hours else "$hours, $d"
}

/** "Selamat pagi/siang/sore/malam" menurut jam lokal perangkat. */
fun greeting(nowMillis: Long = nowEpochMillis(), offsetSeconds: Int = localUtcOffsetSeconds()): String {
    val hour = localParts(nowMillis, offsetSeconds).hour
    return when {
        hour < 11 -> "Selamat pagi"
        hour < 15 -> "Selamat siang"
        hour < 18 -> "Selamat sore"
        else -> "Selamat malam"
    }
}
