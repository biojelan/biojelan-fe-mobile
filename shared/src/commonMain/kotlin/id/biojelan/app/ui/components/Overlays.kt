package id.biojelan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

// ------------------------------------------------------------------ bottom sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = BioTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.paper,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(width = 40.dp, height = 4.dp).background(c.line, RoundedCornerShape(50)))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPad)
                .padding(bottom = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = BioTheme.type.subTitle, color = c.ink, modifier = Modifier.weight(1f))
                CircleIconButton(BioIcons.Close, onDismiss, contentDescription = BioText.current.close)
            }
            content()
        }
    }
}

// ------------------------------------------------------------------ toast

@Stable
class ToastState {
    private var counter = 0L
    var current by mutableStateOf<ToastMessage?>(null)
        private set

    fun show(text: String) {
        counter += 1
        current = ToastMessage(counter, text)
    }

    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }
}

data class ToastMessage(val id: Long, val text: String)

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/** Kumpulkan pesan sekali-pakai dari ViewModel dan tampilkan sebagai toast. */
@Composable
fun CollectMessages(messages: Flow<String>, toast: ToastState) {
    LaunchedEffect(messages) {
        messages.collect { toast.show(it) }
    }
}

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val message = state.current
    if (message != null) {
        LaunchedEffect(message.id) {
            delay(2600)
            state.dismiss(message.id)
        }
        Box(modifier.fillMaxWidth().padding(horizontal = 22.dp), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier
                    .background(c.ink, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(BioIcons.Check, contentDescription = null, tint = c.amber, modifier = Modifier.size(16.dp))
                Text(message.text, style = BioTheme.type.bodyBold, color = c.paper, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

// ------------------------------------------------------------------ state kosong / error / loading

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BioTheme.colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun EmptyBlock(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).background(c.primaryTint, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = BioTheme.type.sectionTitle, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = BioTheme.type.body, color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).background(c.rustTint, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Icon(BioIcons.Alert, contentDescription = null, tint = c.rust, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Gagal memuat data", style = BioTheme.type.sectionTitle, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(message, style = BioTheme.type.body, color = c.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        BioButton(s.retryButton, onRetry, style = BtnStyle.Outline, icon = BioIcons.Refresh)
    }
}
