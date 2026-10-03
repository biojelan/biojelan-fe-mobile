package id.biojelan.app.ui.driver

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/** Titik peta ternormalisasi: x dan y berupa pecahan lebar dan tinggi kartu (0..1). */
data class MapSpot(val x: Float, val y: Float)

/**
 * Proyeksikan koordinat Agen ke kanvas kartu peta. Utara di atas, skala sama di kedua sumbu (bujur
 * dikoreksi dengan cos lintang), dan seluruh titik dimuat dengan margin.
 *
 * Mengembalikan satu entri per masukan. Entri bernilai null untuk koordinat kosong (0,0) dan untuk semua
 * titik bila kurang dari dua koordinat valid atau semuanya berimpit; pemanggil lalu memakai posisi cadangan.
 *
 * @param aspect rasio lebar/tinggi kartu, supaya bentuk sebaran tidak terpelintir.
 */
internal fun projectToMap(points: List<Pair<Double, Double>?>, aspect: Float = 2.2f): List<MapSpot?> {
    val valid = points.map { p -> p?.takeIf { it.first != 0.0 || it.second != 0.0 } }
    val coords = valid.filterNotNull()
    if (coords.size < 2) return List(points.size) { null }

    val meanLat = coords.map { it.first }.average()
    val kx = cos(meanLat * PI / 180.0)
    val xs = coords.map { it.second * kx }
    val ys = coords.map { -it.first } // lintang makin besar = makin utara = makin ke atas
    val spanX = xs.max() - xs.min()
    val spanY = ys.max() - ys.min()
    if (spanX < EPS && spanY < EPS) return List(points.size) { null }

    val cx = (xs.max() + xs.min()) / 2
    val cy = (ys.max() + ys.min()) / 2
    val boxW = (1f - 2 * MARGIN_X) * aspect // lebar tersedia dalam satuan tinggi kartu
    val boxH = 1f - 2 * MARGIN_Y
    val sx = if (spanX < EPS) Double.MAX_VALUE else boxW / spanX
    val sy = if (spanY < EPS) Double.MAX_VALUE else boxH / spanY
    val scale = min(sx, sy)

    return valid.map { p ->
        p?.let {
            val x = 0.5 + (it.second * kx - cx) * scale / aspect
            val y = 0.5 + (-it.first - cy) * scale
            MapSpot(x.toFloat().coerceIn(0f, 1f), y.toFloat().coerceIn(0f, 1f))
        }
    }
}

private const val EPS = 1e-9
private const val MARGIN_X = 0.12f
private const val MARGIN_Y = 0.22f

enum class RoutePhase { Idle, Ready, EnRoute, Arrived, Awaiting, Finished }

/**
 * Posisi driver di rute, diturunkan dari status stop (tanpa GPS): [from] adalah checkpoint terakhir yang
 * sudah dilewati, [to] adalah stop pickup yang sedang berjalan. Keduanya bisa null.
 */
data class RouteProgress(val phase: RoutePhase, val from: RouteStop?, val to: RouteStop?, val currentIndex: Int)

internal fun routeProgress(stops: List<RouteStop>): RouteProgress {
    val ci = stops.indexOfLast { it.isPickup && !it.isFinal }
    if (ci < 0) {
        val phase = if (stops.isEmpty()) RoutePhase.Idle else RoutePhase.Finished
        return RouteProgress(phase, stops.lastOrNull(), null, -1)
    }
    val phase = when (stops[ci].status) {
        StopStatus.OnTheWay -> RoutePhase.EnRoute
        StopStatus.Arrived -> RoutePhase.Arrived
        StopStatus.AwaitingConfirm -> RoutePhase.Awaiting
        else -> RoutePhase.Ready
    }
    return RouteProgress(phase, stops.getOrNull(ci - 1), stops[ci], ci)
}
