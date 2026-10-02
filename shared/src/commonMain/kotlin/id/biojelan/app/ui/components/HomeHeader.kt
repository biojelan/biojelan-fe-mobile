package id.biojelan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.biojelan.app.core.formatTodayLabel
import id.biojelan.app.core.greeting
import id.biojelan.app.data.local.LiveAlerts
import id.biojelan.app.data.local.unreadCount
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import org.koin.compose.koinInject

/**
 * Tiga gaya header Beranda. Semuanya berisi sapaan, nama, peran, tanggal, dan tombol lonceng (toggle notifikasi).
 *
 *  - [Avatar]: tanpa kartu. Avatar + sapaan di kiri, tombol lonceng di kanan. Paling ringan.
 *  - [Graphic]: tanpa kartu dan tanpa border. Isi menempel langsung di latar layar, dihiasi busur kontur,
 *    tetes, wash hijau muda, dan gelombang minyak yang memudar, digambar langsung di latar sampai ke tepi atas layar.
 *    Gaya bawaan; butuh isi Beranda dibungkus [HomeScroll].
 *  - [Card]: kartu gradasi lembut dengan busur kontur dan gelombang minyak (satu bahasa visual dengan kartu stok).
 *  - [Bold]: kartu hijau solid, teks terang. Paling mencolok.
 */
enum class HomeHeaderStyle { Avatar, Graphic, Card, Bold }

/** Ganti [style] di sini untuk mengubah header di Beranda Klien, Agen, dan Driver sekaligus. */
object HomeHeaderDefaults {
    val style: HomeHeaderStyle = HomeHeaderStyle.Graphic
}

/**
 * Header Beranda. [name] sudah berupa nama sapaan (lihat `firstNameOf`), [initials] untuk avatar,
 * [role] label peran. [trailing] opsional. Padding horizontal layar
 * diurus pemanggil (header tidak menambah sendiri).
 *
 * Lonceng membuka [NotificationSheet] (riwayat + toggle pop-up). State dibaca dari [LiveAlerts] yang di-inject,
 * jadi pemanggil tidak perlu meneruskan apa pun.
 */
@Composable
fun HomeHeader(
    name: String,
    initials: String,
    role: String,
    modifier: Modifier = Modifier,
    style: HomeHeaderStyle = HomeHeaderDefaults.style,
    trailing: (@Composable () -> Unit)? = null,
) {
    val alerts: LiveAlerts = koinInject()
    val alertsOn by alerts.enabled.collectAsStateWithLifecycle()
    val items by alerts.items.collectAsStateWithLifecycle()
    var showInbox by remember { mutableStateOf(false) }
    val unread = items.unreadCount
    val onBell = { showInbox = true }
    val hello = greeting() + ","
    val date = formatTodayLabel()
    val outer = modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp)
    when (style) {
        HomeHeaderStyle.Avatar -> AvatarHeader(outer, hello, name, initials, role, date, alertsOn, unread, onBell, trailing)
        HomeHeaderStyle.Graphic -> GraphicHeader(outer, hello, name, initials, role, date, alertsOn, unread, onBell, trailing)
        HomeHeaderStyle.Card -> CardHeader(outer, false, hello, name, initials, role, date, alertsOn, unread, onBell, trailing)
        HomeHeaderStyle.Bold -> CardHeader(outer, true, hello, name, initials, role, date, alertsOn, unread, onBell, trailing)
    }
    if (showInbox) NotificationSheet(onDismiss = { showInbox = false })
}

// ------------------------------------------------------------------ gaya 1: Avatar

@Composable
private fun AvatarHeader(
    modifier: Modifier,
    hello: String,
    name: String,
    initials: String,
    role: String,
    date: String,
    alertsOn: Boolean,
    unread: Int,
    onBell: () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val c = BioTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderAvatar(initials, bold = false)
        Column(Modifier.weight(1f)) {
            Text(hello, style = BioTheme.type.body, color = c.muted)
            Text(name, style = BioTheme.type.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$role · $date", style = BioTheme.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
        AlertsBell(alertsOn, unread, onBell)
    }
}

// ------------------------------------------------------------------ gaya Graphic: tanpa kartu, grafis di latar

/** Posisi header di dalam isi scroll (px), dilaporkan [GraphicHeader] ke [HomeBackdrop]. */
internal class HomeBackdropState {
    var headerTop by mutableFloatStateOf(0f)
    var headerHeight by mutableFloatStateOf(0f)
}

internal val LocalHomeBackdrop = staticCompositionLocalOf<HomeBackdropState?> { null }

/**
 * Pembungkus isi scroll Beranda (Klien, Agen, Driver). Isinya sama seperti `Column` scroll biasa, tetapi di
 * belakangnya ada [HomeBackdrop] yang menggambar grafis header sampai ke tepi atas layar (di balik status bar)
 * dan ikut bergeser saat di-scroll. Container scroll memotong gambar di batasnya, jadi grafis tidak bisa
 * digambar dari dalam header.
 */
@Composable
fun HomeScroll(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    val backdrop = remember { HomeBackdropState() }
    Box(Modifier.fillMaxSize()) {
        HomeBackdrop(scroll, backdrop)
        CompositionLocalProvider(LocalHomeBackdrop provides backdrop) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = ScreenPad).padding(bottom = 24.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun HomeBackdrop(scroll: ScrollState, backdrop: HomeBackdropState) {
    val c = BioTheme.colors
    val statusTop = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    // Tanpa gaya Graphic (mis. Card/Bold) tidak ada yang melapor, jadi tidak ada yang digambar.
    Canvas(Modifier.fillMaxSize()) {
        val hh = backdrop.headerHeight
        if (hh <= 0f) return@Canvas
        val w = size.width
        translate(top = -scroll.value.toFloat()) {
            val top = -statusTop                 // tepi atas layar, dalam koordinat isi scroll
            val hTop = backdrop.headerTop
            val bottom = hTop + hh
            val region = bottom - top
            // wash hijau muda di pojok kanan-atas (pengganti gradasi kartu), memudar ke latar
            val glowR = 230.dp.toPx()
            val glowAt = Offset(w, top + region * 0.10f)
            drawCircle(Brush.radialGradient(listOf(c.primaryTint, Color.Transparent), center = glowAt, radius = glowR), glowR, glowAt)
            // busur kontur, penuh sampai tepi atas layar
            listOf(46f, 80f, 114f, 148f, 182f).forEach { r ->
                drawCircle(
                    c.primary.copy(alpha = 0.12f),
                    radius = r.dp.toPx(),
                    center = Offset(w + 8.dp.toPx(), top + region * 0.18f),
                    style = Stroke(1.5.dp.toPx()),
                )
            }
            // gelombang minyak terisi, memudar ke bawah supaya tidak ada tepi keras
            val back = Brush.verticalGradient(listOf(c.amber.copy(alpha = 0.26f), c.amber.copy(alpha = 0f)), startY = hTop + hh * 0.62f, endY = bottom)
            val front = Brush.verticalGradient(listOf(c.amber.copy(alpha = 0.34f), c.amber.copy(alpha = 0f)), startY = hTop + hh * 0.78f, endY = bottom)
            drawPath(waveFill(w, bottom, hTop + hh * 0.66f, 4.dp.toPx(), 110.dp.toPx(), 0.8f), back)
            drawPath(waveFill(w, bottom, hTop + hh * 0.80f, 3.dp.toPx(), 80.dp.toPx(), 2.6f), front)
            // tetes kecil
            drawCircle(c.amber.copy(alpha = 0.55f), 3.dp.toPx(), Offset(w * 0.66f, hTop + hh * 0.20f))
            drawCircle(c.amber.copy(alpha = 0.38f), 2.dp.toPx(), Offset(w * 0.73f, hTop + hh * 0.34f))
        }
    }
}

@Composable
private fun GraphicHeader(
    modifier: Modifier,
    hello: String,
    name: String,
    initials: String,
    role: String,
    date: String,
    alertsOn: Boolean,
    unread: Int,
    onBell: () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val c = BioTheme.colors
    val backdrop = LocalHomeBackdrop.current
    // Grafisnya digambar [HomeBackdrop] di belakang scroll (supaya bisa sampai ke tepi atas layar);
    // header cuma melaporkan posisinya agar gelombang menempel di dasar header.
    Box(
        modifier.onGloballyPositioned { coords ->
            backdrop?.let {
                it.headerTop = coords.positionInParent().y
                it.headerHeight = coords.size.height.toFloat()
            }
        },
    ) {
        Column(Modifier.padding(bottom = 30.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeaderAvatar(initials, bold = false)
                Column(Modifier.weight(1f)) {
                    Text(hello, style = BioTheme.type.body, color = c.muted)
                    Text(name, style = BioTheme.type.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                trailing?.invoke()
                AlertsBell(alertsOn, unread, onBell)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BioChip(role, ChipKind.Open)
                Text(date, style = BioTheme.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ------------------------------------------------------------------ gaya Card / Bold

@Composable
private fun CardHeader(
    modifier: Modifier,
    bold: Boolean,
    hello: String,
    name: String,
    initials: String,
    role: String,
    date: String,
    alertsOn: Boolean,
    unread: Int,
    onBell: () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val c = BioTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val titleColor = if (bold) c.onPrimary else c.ink
    val softColor = if (bold) c.onPrimary.copy(alpha = 0.78f) else c.muted
    val brush = if (bold) Brush.linearGradient(listOf(c.primary, c.primaryDeep)) else Brush.linearGradient(listOf(c.surface, c.primaryTint))

    Box(
        modifier
            .clip(shape)
            .background(brush, shape)
            .let { if (bold) it else it.border(1.dp, c.line, shape) },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val line = if (bold) c.onPrimary else c.primary
            // busur kontur di pojok kanan-atas
            listOf(46f, 80f, 114f).forEach { r ->
                drawCircle(
                    line.copy(alpha = if (bold) 0.10f else 0.06f),
                    radius = r.dp.toPx(),
                    center = Offset(w * 0.97f, -h * 0.10f),
                    style = Stroke(1.5.dp.toPx()),
                )
            }
            // gelombang minyak tipis di dasar kartu
            drawPath(waveFill(w, h, h * 0.84f, 4.dp.toPx(), 110.dp.toPx(), 0.8f), c.amber.copy(alpha = if (bold) 0.16f else 0.11f))
            drawPath(waveFill(w, h, h * 0.92f, 3.dp.toPx(), 80.dp.toPx(), 2.6f), c.amber.copy(alpha = if (bold) 0.22f else 0.17f))
            // tetes kecil
            drawCircle(c.amber.copy(alpha = if (bold) 0.55f else 0.38f), 3.dp.toPx(), Offset(w * 0.66f, h * 0.20f))
            drawCircle(c.amber.copy(alpha = if (bold) 0.40f else 0.26f), 2.dp.toPx(), Offset(w * 0.73f, h * 0.34f))
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeaderAvatar(initials, bold)
                Column(Modifier.weight(1f)) {
                    Text(hello, style = BioTheme.type.body, color = softColor)
                    Text(name, style = BioTheme.type.headline, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                trailing?.invoke()
                AlertsBell(alertsOn, unread, onBell)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bold) RoleTag(role) else BioChip(role, ChipKind.Open)
                Text(date, style = BioTheme.type.small, color = softColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ------------------------------------------------------------------ bagian kecil

@Composable
private fun HeaderAvatar(initials: String, bold: Boolean) {
    val c = BioTheme.colors
    val bg = if (bold) c.onPrimary.copy(alpha = 0.16f) else c.primary
    Box(
        Modifier
            .size(46.dp)
            .background(bg, CircleShape)
            .border(2.dp, if (bold) c.onPrimary.copy(alpha = 0.35f) else c.primaryTint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = BioTheme.type.sectionTitle, color = c.onPrimary)
    }
}

/** Label peran untuk kartu hijau (BioChip bawaan berwarna terang, kurang cocok di atas hijau pekat). */
@Composable
private fun RoleTag(text: String) {
    val c = BioTheme.colors
    Box(Modifier.background(c.onPrimary.copy(alpha = 0.16f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = BioTheme.type.chip, color = c.onPrimary, maxLines = 1)
    }
}

/**
 * Tombol lonceng bulat; ketuk untuk membuka kotak masuk. Ikon lonceng dicoret = pop-up mati. Lencana angka =
 * jumlah notifikasi belum dibaca (maks "9+"), muncul walau pop-up mati.
 */
@Composable
private fun AlertsBell(popupOn: Boolean, unread: Int, onClick: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(38.dp)
                .background(c.surface, CircleShape)
                .border(1.dp, c.line, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (popupOn) BioIcons.Bell else BioIcons.BellOff,
                contentDescription = s.notificationsTitle,
                tint = if (popupOn) c.primary else c.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        if (unread > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                    .border(1.5.dp, c.surface, CircleShape)
                    .background(c.rust, CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (unread > 9) "9+" else unread.toString(), style = BioTheme.type.caption.copy(fontSize = 9.5.sp), color = Color.White, maxLines = 1)
            }
        }
    }
}