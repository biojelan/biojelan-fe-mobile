package id.biojelan.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.biojelan.app.resources.Res
import id.biojelan.app.resources.login_hero
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

// ------------------------------------------------------------------ transisi Masuk <-> Daftar

/** Satu durasi + kurva untuk SEMUA bagian yang bergeser saat ganti mode, supaya hero & form bergerak serempak. */
internal const val AuthModeMillis = 360
internal val AuthModeEasing = FastOutSlowInEasing

// ------------------------------------------------------------------ entrance

/**
 * Muncul bertahap: fade + geser sedikit dari bawah (+ opsional zoom halus). [index] menentukan jeda
 * antar elemen. Hanya menyentuh graphicsLayer, jadi tidak memicu relayout (tinggi form tetap stabil).
 */
@Composable
internal fun Modifier.entrance(
    index: Int,
    distance: Dp = 16.dp,
    scaleFrom: Float = 1f,
    stepMillis: Long = 55L,
): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis)
        progress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    val distancePx = with(LocalDensity.current) { distance.toPx() }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distancePx
        val sc = scaleFrom + (1f - scaleFrom) * p
        scaleX = sc
        scaleY = sc
    }
}

// ------------------------------------------------------------------ hero

/**
 * Gaya hero halaman masuk.
 * - [Bleed]: terang, penuh lebar sampai ke tepi atas layar (di balik status bar), gambar digambar native
 *   (ikut tema terang/gelap). Paling hemat tempat.
 * - [Card]: kartu hijau tua bersudut membulat (aset `login_hero.jpg`), versi pendek.
 */
enum class AuthHeroStyle { Bleed, Card }

/** Ganti nilai ini untuk pindah gaya. */
internal val CurrentAuthHeroStyle = AuthHeroStyle.Bleed

// Koordinat aset login_hero.jpg (1320×880). Semua gambar animasi dipetakan dari ruang gambar ini
// ke ruang kotak hero, mengikuti ContentScale.Crop supaya tetap sejajar dengan latar.
private const val IMG_W = 1320f
private const val IMG_H = 880f
private const val ORIGIN_X = 1003f
private const val ORIGIN_Y = 470f
private const val FLAT = 0.78f          // permukaan dilihat miring -> lingkaran jadi elips
private const val DIAL_R = 176f
private const val DIAL_TICKS = 144
private const val HOT_START = 20
private const val DROP_SCALE = 0.95f
private const val FALL_DISTANCE = 430f   // jatuh dari luar bingkai atas
private const val RIPPLE_MIN = 34f
private const val RIPPLE_MAX = 340f

// Timing jatuhnya tetesan. Ubah FALL_MILLIS untuk lebih cepat/lambat; splash & riak otomatis menyusul.
private const val DROP_DELAY_MILLIS = 260L
private const val FALL_MILLIS = 1200            // sebelumnya 560
private const val SPLASH_MILLIS = 1100
private const val RIPPLE_MILLIS = 7200          // sebelumnya 4200 (makin besar = riak makin lambat)
private const val RIPPLE_COUNT = 2              // sebelumnya 3 (lebih sedikit = tidak saling tumpuk)

private val Gold = Color(0xFFF0C35E)
private val GoldMid = Color(0xFFD69A2E)
private val GoldDeep = Color(0xFFA8701D)

private fun dropPath(): Path = Path().apply {
    moveTo(50f, 6f)
    cubicTo(31f, 33f, 12f, 57f, 12f, 80f)
    arcTo(Rect(12f, 42f, 88f, 118f), 180f, -180f, false)
    cubicTo(88f, 57f, 69f, 33f, 50f, 6f)
    close()
}

private fun highlightPath(): Path = Path().apply {
    moveTo(33f, 61f)
    cubicTo(24f, 72f, 23f, 85f, 32f, 94f)
    cubicTo(29f, 84f, 29f, 73f, 36f, 63f)
    close()
}

private const val BLEED_K = 0.5f        // dp per unit-gambar untuk gaya Bleed (menentukan ukuran tetesan)
private const val BLEED_ORIGIN_RIGHT = 76f  // jarak titik jatuh dari tepi kanan (dp)
private const val BLEED_ORIGIN_BELOW_STATUS = 74f

/** Titik jatuh + skala: semua gambar hero dihitung relatif terhadap ini. */
private class HeroFrame(val cx: Float, val cy: Float, val k: Float)

private fun DrawScope.heroFrame(bleed: Boolean, statusTopPx: Float): HeroFrame =
    if (bleed) {
        HeroFrame(
            cx = size.width - BLEED_ORIGIN_RIGHT.dp.toPx(),
            cy = statusTopPx + BLEED_ORIGIN_BELOW_STATUS.dp.toPx(),
            k = BLEED_K * density,
        )
    } else {
        // ruang gambar -> ruang kotak (setara ContentScale.Crop, rata tengah)
        val k = max(size.width / IMG_W, size.height / IMG_H)
        HeroFrame(
            cx = (size.width - IMG_W * k) / 2f + ORIGIN_X * k,
            cy = (size.height - IMG_H * k) / 2f + ORIGIN_Y * k,
            k = k,
        )
    }

/** Latar statis gaya Bleed: pendar tint, cincin konsentris, dan skala dial. Tidak membaca state -> tidak digambar ulang tiap frame. */
private fun DrawScope.drawBleedBackdrop(f: HeroFrame, tint: Color, ring: Color, accent: Color) {
    val center = Offset(f.cx, f.cy)
    val glowR = 260.dp.toPx()
    drawCircle(Brush.radialGradient(listOf(tint, tint.copy(alpha = 0f)), center = center, radius = glowR), glowR, center)

    var r = 150f
    var st = 12.5f
    var n = 0
    while (r < 760f) {
        val a = 0.34f * exp(-(r - 150f) / 380f) + 0.03f
        val major = n % 5 == 0
        drawOval(
            color = if (major) accent.copy(alpha = (a * 1.9f).coerceAtMost(1f)) else ring.copy(alpha = a * 0.9f),
            topLeft = Offset(f.cx - r * f.k, f.cy - FLAT * r * f.k),
            size = Size(2f * r * f.k, 2f * FLAT * r * f.k),
            style = Stroke(width = (if (major) 1.6f else 1f).dp.toPx()),
        )
        r += st
        st *= 1.034f
        n++
    }

    val twoPi = (2.0 * PI).toFloat()
    drawOval(
        color = accent.copy(alpha = 0.28f),
        topLeft = Offset(f.cx - DIAL_R * f.k, f.cy - FLAT * DIAL_R * f.k),
        size = Size(2f * DIAL_R * f.k, 2f * FLAT * DIAL_R * f.k),
        style = Stroke(width = 1.dp.toPx()),
    )
    for (i in 0 until DIAL_TICKS) {
        val th = twoPi * i / DIAL_TICKS - (PI / 2).toFloat()
        val major = i % 6 == 0
        val r2 = DIAL_R + if (major) 16f else 8f
        drawLine(
            color = accent.copy(alpha = if (major) 0.55f else 0.30f),
            start = Offset(f.cx + DIAL_R * cos(th) * f.k, f.cy + FLAT * DIAL_R * sin(th) * f.k),
            end = Offset(f.cx + r2 * cos(th) * f.k, f.cy + FLAT * r2 * sin(th) * f.k),
            strokeWidth = 1.1f.dp.toPx(),
        )
    }
}

/**
 * Hero di atas form masuk/daftar, lockup "BioJelan" di kiri atas. Lapisan animasi (digambar native):
 * tetesan jatuh dari atas lalu memantul kecil, cincin kejut sekali, riak yang terus menyebar,
 * tetesan melayang pelan, dan satu jarum emas yang menyapu dial. Lebih pendek saat mode Daftar.
 */
@Composable
internal fun AuthHero(
    compact: Boolean,
    modifier: Modifier = Modifier,
    style: AuthHeroStyle = CurrentAuthHeroStyle,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val bleed = style == AuthHeroStyle.Bleed
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusTopPx = with(LocalDensity.current) { statusTop.toPx() }

    val loop = rememberInfiniteTransition(label = "hero-loop")
    val ripple = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(RIPPLE_MILLIS, easing = LinearEasing)), label = "ripple")
    val sweep = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(36000, easing = LinearEasing)), label = "sweep")
    val bob = loop.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "bob")

    val fall = remember { Animatable(0f) }       // 0 = di atas bingkai, 1 = mendarat
    val impact = remember { Animatable(0f) }     // pantulan kecil setelah mendarat
    val splash = remember { Animatable(0f) }     // cincin kejut satu kali saat mendarat
    val rippleGate = remember { Animatable(0f) } // riak berulang baru "menyala" setelah mendarat
    LaunchedEffect(Unit) {
        delay(DROP_DELAY_MILLIS)
        fall.animateTo(1f, tween(FALL_MILLIS, easing = CubicBezierEasing(0.5f, 0f, 0.9f, 0.5f)))
        impact.snapTo(1f)
        impact.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(DROP_DELAY_MILLIS + FALL_MILLIS)
        splash.animateTo(1f, tween(SPLASH_MILLIS, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        delay(DROP_DELAY_MILLIS + FALL_MILLIS + SPLASH_MILLIS)   // riak mulai setelah cincin kejut selesai
        rippleGate.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    val drop = remember { dropPath() }
    val highlight = remember { highlightPath() }

    // Gold di atas hijau tua (Card); amber tema di atas kertas (Bleed) supaya kontras di terang & gelap.
    val accent = if (bleed) c.amber else Gold
    val paper = c.paper
    val haloAlpha = if (bleed) 0.22f else 0.30f

    // Tinggi/rasio dianimasikan eksplisit (tween) -- bukan animateContentSize berpegas -- agar selaras dengan form di bawahnya.
    val heroHeight by animateDpAsState(
        statusTop + if (compact) 112.dp else 150.dp,
        tween(AuthModeMillis, easing = AuthModeEasing),
        label = "hero-height",
    )
    val heroRatio by animateFloatAsState(
        if (compact) 3.2f else 2.5f,
        tween(AuthModeMillis, easing = AuthModeEasing),
        label = "hero-ratio",
    )
    val frame = if (bleed) {
        modifier.fillMaxWidth().height(heroHeight).clipToBounds()  // Canvas tidak clip otomatis: tanpa ini ring tembus ke form
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(heroRatio)
            .clip(RoundedCornerShape(24.dp))
    }

    Box(modifier = frame) {
        // ---- latar statis
        if (bleed) {
            val tint = c.primaryTint
            val ring = c.primary
            Canvas(Modifier.matchParentSize()) {
                drawBleedBackdrop(heroFrame(true, statusTopPx), tint, ring, accent)
            }
        } else {
            Image(
                painter = painterResource(Res.drawable.login_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        // ---- lapisan animasi
        Canvas(Modifier.matchParentSize()) {
            val f = heroFrame(bleed, statusTopPx)
            val cx = f.cx
            val cy = f.cy
            val k = f.k
            val twoPi = (2.0 * PI).toFloat()
            val gate = rippleGate.value

            // riak: tiga gelombang berselang, melebar lalu memudar
            for (i in 0 until RIPPLE_COUNT) {
                val p = (ripple.value + i / RIPPLE_COUNT.toFloat()) % 1f
                val r = RIPPLE_MIN + (RIPPLE_MAX - RIPPLE_MIN) * (1f - (1f - p) * (1f - p))
                val a = (1f - p).pow(1.6f) * 0.8f * gate
                drawOval(
                    color = accent.copy(alpha = a),
                    topLeft = Offset(cx - r * k, cy - FLAT * r * k),
                    size = Size(2f * r * k, 2f * FLAT * r * k),
                    style = Stroke(width = (2.4f - 1.4f * p) * k.coerceAtLeast(0.5f)),
                )
            }

            // cincin kejut satu kali saat tetesan menyentuh permukaan
            val sp = splash.value
            if (sp > 0f && sp < 1f) {
                val r = 30f + 250f * (1f - (1f - sp) * (1f - sp))
                drawOval(
                    color = accent.copy(alpha = (1f - sp).pow(1.4f) * 0.95f),
                    topLeft = Offset(cx - r * k, cy - FLAT * r * k),
                    size = Size(2f * r * k, 2f * FLAT * r * k),
                    style = Stroke(width = (3.2f - 2.2f * sp) * k.coerceAtLeast(0.5f)),
                )
            }

            // jarum dial: satu tick terang menyapu pelan, dengan ekor memudar
            val step = twoPi / DIAL_TICKS
            val head = twoPi * HOT_START / DIAL_TICKS - (PI / 2).toFloat() + twoPi * sweep.value
            for (j in 0..5) {
                val th = head - j * step
                val r1 = DIAL_R
                val r2 = DIAL_R + if (j == 0) 20f else 12f
                drawLine(
                    color = accent.copy(alpha = (1f - j / 6f) * gate),
                    start = Offset(cx + r1 * cos(th) * k, cy + FLAT * r1 * sin(th) * k),
                    end = Offset(cx + r2 * cos(th) * k, cy + FLAT * r2 * sin(th) * k),
                    strokeWidth = (if (j == 0) 2f else 1.3f).dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // tetesan: jatuh + memantul, lalu melayang naik-turun pelan
            val fv = fall.value
            val dropAlpha = (fv * 6f).coerceIn(0f, 1f)
            val floatY = sin(bob.value * twoPi) * 2.5f
            val dy = (fv - 1f) * FALL_DISTANCE + impact.value * 7f + floatY
            val breath = 0.85f + 0.15f * sin(bob.value * twoPi + 1f)
            val s0 = DROP_SCALE * k

            // pendar di sekitar tetesan
            val haloCenter = Offset(cx, cy + (-69f + dy) * k)
            val haloR = 125f * k
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldMid.copy(alpha = haloAlpha * breath * dropAlpha), GoldMid.copy(alpha = 0f)),
                    center = haloCenter,
                    radius = haloR,
                ),
                radius = haloR,
                center = haloCenter,
            )

            // bayangan di permukaan (muncul saat tetesan mendekat)
            val near = fv.coerceIn(0f, 1f).pow(6f)
            withTransform({
                translate(cx, cy + (16f - dy) * 0.34f * k)
                scale(s0, -s0 * 0.34f, Offset.Zero)
                translate(-50f, -118f)
            }) {
                drawPath(drop, Gold, alpha = 0.14f * near)
            }

            // badan tetesan
            withTransform({
                translate(cx, cy + (-16f + dy) * k)
                scale(s0, s0, Offset.Zero)
                translate(-50f, -118f)
            }) {
                drawPath(
                    path = drop,
                    brush = Brush.linearGradient(
                        colors = listOf(Gold, GoldMid, GoldDeep),
                        start = Offset(0f, 6f),
                        end = Offset(0f, 118f),
                    ),
                    alpha = dropAlpha,
                )
                drawPath(highlight, Color.White, alpha = 0.36f * dropAlpha)
            }

            if (bleed) {
                // sisi kiri dibuat tenang supaya lockup terbaca, dan dasar hero larut ke latar halaman (tanpa tepi keras)
                drawRect(
                    Brush.horizontalGradient(
                        listOf(paper.copy(alpha = 0.88f), paper.copy(alpha = 0f)),
                        startX = 0f,
                        endX = size.width * 0.62f,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(paper.copy(alpha = 0f), paper),
                        startY = size.height * 0.60f,
                        endY = size.height,
                    ),
                )
            }
        }

        // ---- lockup di atas
        val onHero = if (bleed) c.ink else Color.White
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = if (bleed) ScreenPad else 18.dp,
                    top = if (bleed) statusTop + 14.dp else 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 36)
            Spacer(Modifier.width(10.dp))
            Text(s.appName, style = BioTheme.type.title, color = onHero)
        }
    }
}