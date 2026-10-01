package id.biojelan.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ikon garis 24×24 gaya prototype (stroke 1.9, ujung bulat). Semuanya digambar hitam lalu diberi
 * warna lewat `tint` pada `Icon`. Path ditulis dengan flag arc berspasi agar aman untuk parser Compose.
 */
object BioIcons {
    private fun icon(name: String, vararg paths: String, filled: Boolean = false, stroke: Float = 1.9f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = if (filled) null else SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Drop by lazy { icon("drop", "M12 2C8 8 4 12.2 4 16a8 8 0 0 0 16 0c0-3.8-4-8-8-14z", stroke = 1.7f) }
    val DropFilled by lazy { icon("dropFilled", "M12 2C8 8 4 12.2 4 16a8 8 0 0 0 16 0c0-3.8-4-8-8-14z", filled = true) }
    val Pin by lazy { icon("pin", "M12 21s-7-6.1-7-11a7 7 0 0 1 14 0c0 4.9-7 11-7 11z", "M12 7.6a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 1 0 0-4.8z") }
    val Clock by lazy { icon("clock", "M12 3.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 1 0 0-17z", "M12 8v4l3 2") }
    val Chat by lazy { icon("chat", "M21 11.5a8.5 8.5 0 0 1-12.4 7.6L4 20l1-4.4A8.5 8.5 0 1 1 21 11.5z") }
    val Search by lazy { icon("search", "M11 4a7 7 0 1 0 0 14 7 7 0 1 0 0-14z", "M21 21l-4.3-4.3") }
    val Back by lazy { icon("back", "M15 6l-6 6 6 6", stroke = 2.2f) }
    val Chevron by lazy { icon("chevron", "M9 6l6 6-6 6", stroke = 2.2f) }
    val Mail by lazy { icon("mail", "M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M3 7l9 6 9-6") }
    val Phone by lazy { icon("phone", "M4 5c0 8 7 15 15 15l2-4-5-2-2 2c-2-1-4-3-5-5l2-2-2-5z") }
    val Filter by lazy { icon("filter", "M4 6h16M7 12h10M10 18h4") }
    val Plus by lazy { icon("plus", "M12 5v14M5 12h14", stroke = 2.4f) }
    val Check by lazy { icon("check", "M20 6L9 17l-5-5", stroke = 2.2f) }
    val Close by lazy { icon("close", "M6 6l12 12M18 6L6 18", stroke = 2.2f) }
    val Home by lazy { icon("home", "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", "M9 22V12h6v10") }
    val Receipt by lazy { icon("receipt", "M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01", stroke = 2.2f) }
    val User by lazy { icon("user", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2", "M12 3a4 4 0 1 0 0 8 4 4 0 1 0 0-8z") }
    val Info by lazy { icon("info", "M12 3a9 9 0 1 0 0 18 9 9 0 1 0 0-18z", "M12 16v-4M12 8h.01") }
    val Alert by lazy { icon("alert", "M12 3a9 9 0 1 0 0 18 9 9 0 1 0 0-18z", "M12 9v4M12 17h.01") }
    val Lock by lazy { icon("lock", "M5 11h14v10H5z", "M8 11V7a4 4 0 0 1 8 0v4") }
    val Logout by lazy { icon("logout", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17l5-5-5-5", "M21 12H9") }
    val Refresh by lazy { icon("refresh", "M21 12a9 9 0 1 1-3-6.7L21 8", "M21 3v5h-5") }
    val Eye by lazy { icon("eye", "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z", "M12 9a3 3 0 1 0 0 6 3 3 0 1 0 0-6z") }
    val EyeOff by lazy { icon("eyeOff", "M17.9 17.9A10 10 0 0 1 12 19c-7 0-10-7-10-7a18.5 18.5 0 0 1 5-5.9", "M9.9 4.2A9 9 0 0 1 12 4c7 0 10 7 10 7a18.5 18.5 0 0 1-2.2 3.2", "M1 1l22 22") }
    val Bell by lazy { icon("bell", "M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9", "M13.7 21a2 2 0 0 1-3.4 0") }
    val Trash by lazy { icon("trash", "M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6") }
    val Edit by lazy { icon("edit", "M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z") }
    val Store by lazy { icon("store", "M3 9l1-5h16l1 5", "M3 9v11h18V9", "M3 9h18", "M9 20v-6h6v6") }
    val Bank by lazy { icon("bank", "M3 10l9-6 9 6", "M5 10v8M9 10v8M15 10v8M19 10v8", "M3 21h18") }
    val IdCard by lazy { icon("idcard", "M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z", "M8 11a2 2 0 1 0 0 4 2 2 0 1 0 0-4z", "M14 10h4M14 14h4") }
    val ArrowUp by lazy { icon("arrowUp", "M12 19V5M5 12l7-7 7 7", stroke = 2f) }
    val Map by lazy { icon("map", "M1 6l7-3 8 3 7-3v15l-7 3-8-3-7 3z", "M8 3v15M16 6v15") }
    val Calendar by lazy { icon("calendar", "M5 4h14a2 2 0 0 1 2 2v14H3V6a2 2 0 0 1 2-2z", "M3 10h18M8 2v4M16 2v4") }
    val Gauge by lazy { icon("gauge", "M12 3C8 8 5 11.5 5 15a7 7 0 0 0 14 0c0-3.5-3-7-7-12z", "M9 16a3 3 0 0 0 3 3") }
    val Truck by lazy {
        icon(
            "truck",
            "M2 8h11v9H2z",
            "M13 11h4l4 3.5V17h-8z",
            "M6.5 20a2 2 0 1 0 0-4 2 2 0 1 0 0 4z",
            "M16.5 20a2 2 0 1 0 0-4 2 2 0 1 0 0 4z",
        )
    }
}
