package id.biojelan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.biojelan.app.core.formatRelativeMillis
import id.biojelan.app.data.local.AlertItem
import id.biojelan.app.data.local.LiveAlerts
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import org.koin.compose.koinInject

/**
 * Kotak masuk notifikasi (dibuka dari lonceng di header Beranda): toggle pop-up di atas, lalu riwayat
 * terbaru dulu. Entri yang belum dibaca saat sheet dibuka ditandai; semuanya otomatis dianggap terbaca
 * begitu sheet ditutup.
 */
@Composable
fun NotificationSheet(onDismiss: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    val alerts: LiveAlerts = koinInject()
    val items by alerts.items.collectAsStateWithLifecycle()
    val popupOn by alerts.enabled.collectAsStateWithLifecycle()
    // Dibekukan saat dibuka: kejadian baru yang tiba selagi sheet terbuka tidak ikut "terbaca" diam-diam.
    val freshIds = remember { alerts.items.value.filter { !it.read }.map { it.id }.toSet() }

    BioSheet(
        title = s.notificationsTitle,
        onDismiss = {
            alerts.markAllRead()
            onDismiss()
        },
    ) {
        Column(Modifier.fillMaxWidth().bioCard(16.dp)) {
            ProfileRow(
                if (popupOn) BioIcons.Bell else BioIcons.BellOff, s.liveAlertsTitle, value = s.liveAlertsHint,
                onClick = alerts::toggle,
                trailing = { BioSwitch(checked = popupOn, onCheckedChange = alerts::setEnabled) },
            )
        }

        Spacer(Modifier.height(16.dp))
        if (items.isEmpty()) {
            EmptyInbox()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item -> AlertRow(item, fresh = item.id in freshIds) }
            }
            Text(
                s.notificationsClear,
                style = BioTheme.type.label,
                color = c.rust,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = alerts::clearInbox)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun AlertRow(item: AlertItem, fresh: Boolean) {
    val c = BioTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .bioCard(14.dp, background = if (fresh) c.primaryTint else Color.Unspecified)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).background(if (fresh) c.surface else c.primaryTint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(BioIcons.Bell, contentDescription = null, tint = c.primary, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(item.text, style = BioTheme.type.bodyBold, color = c.ink)
            Text(formatRelativeMillis(item.at), style = BioTheme.type.small, color = c.muted)
        }
        if (fresh) Box(Modifier.size(8.dp).background(c.primary, CircleShape))
    }
}

@Composable
private fun EmptyInbox() {
    val c = BioTheme.colors
    val s = BioText.current
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(c.primaryTint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(BioIcons.Bell, contentDescription = null, tint = c.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(s.notificationsEmpty, style = BioTheme.type.sectionTitle, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(s.notificationsEmptyHint, style = BioTheme.type.small, color = c.muted, textAlign = TextAlign.Center)
    }
}
