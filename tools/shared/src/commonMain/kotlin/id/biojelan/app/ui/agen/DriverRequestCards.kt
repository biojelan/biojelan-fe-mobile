package id.biojelan.app.ui.agen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/** Kartu di Beranda Agen: Driver ingin mengambil minyak; Agen menerima atau menolak. */
@Composable
fun DriverRequestCard(
    tx: DriverTransactionDto,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = BioText.current
    DriverActionCard(
        title = s.driverRequestTitle,
        body = s.driverRequestBody(formatLiter(tx.volumeLiter)),
        total = tx.totalPrice,
        busy = busy,
        negativeLabel = s.reject,
        negativeStyle = BtnStyle.Rust,
        onNegative = onReject,
        positiveLabel = s.accept,
        positiveStyle = BtnStyle.Primary,
        onPositive = onAccept,
        modifier = modifier,
    )
}

/** Kartu di Beranda Agen: Driver meminta membatalkan pengambilan; Agen menyetujui atau menolak. */
@Composable
fun DriverCancelCard(
    tx: DriverTransactionDto,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = BioText.current
    DriverActionCard(
        title = s.cancelRequestTitle,
        body = s.driverCancelBody(formatLiter(tx.volumeLiter)),
        total = tx.totalPrice,
        busy = busy,
        negativeLabel = s.rejectCancel,
        negativeStyle = BtnStyle.Outline,
        onNegative = onReject,
        positiveLabel = s.acceptCancel,
        positiveStyle = BtnStyle.Rust,
        onPositive = onAccept,
        modifier = modifier,
    )
}

@Composable
private fun DriverActionCard(
    title: String,
    body: String,
    total: Long,
    busy: Boolean,
    negativeLabel: String,
    negativeStyle: BtnStyle,
    onNegative: () -> Unit,
    positiveLabel: String,
    positiveStyle: BtnStyle,
    onPositive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = BioTheme.colors
    Column(
        modifier.fillMaxWidth().bioCard(20.dp, background = c.amberTint, border = c.amber.copy(alpha = 0.5f)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(BioIcons.Bell, contentDescription = null, tint = c.amberDeep, modifier = Modifier.size(18.dp))
            Text(title, style = BioTheme.type.sectionTitle, color = c.amberText)
        }
        Spacer(Modifier.height(8.dp))
        Text(body, style = BioTheme.type.body, color = c.amberText)
        Spacer(Modifier.height(4.dp))
        Text(formatRupiah(total), style = BioTheme.type.monoLarge, color = c.amberText)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BioButton(negativeLabel, onNegative, Modifier.weight(1f), style = negativeStyle, enabled = !busy)
            BioButton(positiveLabel, onPositive, Modifier.weight(1f), style = positiveStyle, loading = busy, icon = BioIcons.Check)
        }
    }
}
