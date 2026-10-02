package id.biojelan.app.ui.klien

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.text.style.TextOverflow
import id.biojelan.app.core.formatClock
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.CircleIconButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.biojelan.app.core.agenInitials
import id.biojelan.app.core.formatOperatingHours
import id.biojelan.app.core.googleMapsDirectionsUrl
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.ui.components.AvatarBox
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.EmptyBlock
import id.biojelan.app.ui.components.InfoItem
import id.biojelan.app.ui.components.MapPreview
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SubHeader
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import org.koin.compose.koinInject

private val MapBodyHeight = 210.dp
private val CardOverlap = 44.dp

/**
 * Detail Agen — memakai cache dari GET /api/user/agen (tidak ada endpoint detail di API-DOC).
 * Peta full-bleed di atas (menjangkau bawah status bar) dan kartu Agen menimpa dasarnya.
 */
@Composable
fun AgenDetailScreen(agenId: String, onBack: () -> Unit, users: UserRepository = koinInject()) {
    val c = BioTheme.colors
    val s = BioText.current
    val agens by users.agens.collectAsStateWithLifecycle()
    val agen = agens.firstOrNull { it.agenId == agenId }
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize().background(c.paper).navigationBarsPadding()) {
        if (agen == null) {
            SubHeader("Detail Agen", onBack, Modifier.statusBarsPadding())
            EmptyBlock(BioIcons.Store, "Agen tidak ditemukan", "Kembali dan muat ulang daftar Agen.")
            return@Column
        }
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth()) {
                MapPreview(
                    points = listOf(agen.latitude to agen.longitude),
                    height = MapBodyHeight + statusBar,
                    cornerRadius = 0.dp,
                )
                CircleIconButton(
                    BioIcons.Back,
                    onBack,
                    modifier = Modifier.padding(start = 16.dp, top = statusBar + 10.dp),
                    contentDescription = "Kembali",
                )
            }

            // Isi di bawah peta ditarik naik sebesar CardOverlap; padding bawah mengganti ruang yang hilang.
            Column(
                Modifier
                    .offset(y = -CardOverlap)
                    .padding(horizontal = ScreenPad)
                    .padding(bottom = 16.dp + CardOverlap),
            ) {
                Row(
                    Modifier.fillMaxWidth().bioCard(20.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AvatarBox(agenInitials(agen.name), size = 52.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(agen.name, style = BioTheme.type.headline, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BioChip(if (agen.isOpen) s.open else s.closed, if (agen.isOpen) ChipKind.Open else ChipKind.Closed)
                            val next = if (agen.isOpen) agen.closeAt else agen.openAt
                            if (next.isNotBlank()) {
                                Text(
                                    (if (agen.isOpen) "Tutup pukul " else "Buka pukul ") + formatClock(next),
                                    style = BioTheme.type.small,
                                    color = c.muted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().bioCard(16.dp)) {
                    InfoItem(BioIcons.Pin, s.labelAddress, agen.address.ifBlank { "-" })
                    InfoItem(BioIcons.Clock, s.labelOperatingHours, formatOperatingHours(agen.openAt, agen.closeAt, agen.openDays))
                    InfoItem(BioIcons.Phone, s.labelPhone, agen.phone.ifBlank { "-" })
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenPad).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BioButton(
                "Rute ke Agen",
                onClick = { uriHandler.openUri(googleMapsDirectionsUrl(agen.latitude, agen.longitude, agen.address)) },
                modifier = Modifier.weight(1f),
                style = BtnStyle.Outline,
                icon = BioIcons.Map,
            )
            BioButton(
                "Hubungi Agen",
                onClick = { uriHandler.openUri(whatsappUrl(agen.phone, agen.name)) },
                modifier = Modifier.weight(1f),
                enabled = agen.phone.isNotBlank(),
                icon = BioIcons.Chat,
            )
        }
    }
}

/** Nomor lokal 08xx -> 628xx untuk wa.me. */
private fun whatsappUrl(phone: String, agenName: String): String {
    val digits = phone.filter { it.isDigit() }
    val intl = when {
        digits.startsWith("62") -> digits
        digits.startsWith("0") -> "62" + digits.drop(1)
        else -> digits
    }
    val text = "Halo ${agenName.trim().replace(' ', '+')}, saya ingin menjual minyak jelantah lewat BioJelan."
    return "https://wa.me/$intl?text=${text.replace(",", "%2C")}"
}