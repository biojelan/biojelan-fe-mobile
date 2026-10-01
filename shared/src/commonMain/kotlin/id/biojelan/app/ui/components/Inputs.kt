package id.biojelan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.theme.BioTheme

@Composable
fun BioField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    hint: String? = null,
    error: String? = null,
    onDone: (() -> Unit)? = null,
) {
    val c = BioTheme.colors
    var focused by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        error != null -> c.rust
        focused -> c.primary
        else -> c.line
    }

    Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label, style = BioTheme.type.label, color = c.inkSoft, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (readOnly) c.primaryTint else c.surface, shape)
                .border(1.5.dp, borderColor, shape)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                textStyle = BioTheme.type.body.copy(
                    color = if (readOnly) c.primary else c.ink,
                    fontSize = 14.5.sp,
                ),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                visualTransformation = if (isPassword && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = BioTheme.type.body.copy(fontSize = 14.5.sp), color = c.muted)
                        }
                        inner()
                    }
                },
            )
            if (isPassword) {
                Icon(
                    imageVector = if (revealed) BioIcons.EyeOff else BioIcons.Eye,
                    contentDescription = if (revealed) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                    tint = c.muted,
                    modifier = Modifier.padding(start = 8.dp).size(20.dp).clip(RoundedCornerShape(6.dp)).clickable { revealed = !revealed },
                )
            }
        }
        val footer = error ?: hint
        if (footer != null) {
            Text(
                footer,
                style = BioTheme.type.small,
                color = if (error != null) c.rust else c.muted,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

/** Segmented control 2+ opsi (Masuk | Daftar, Semua | Buka sekarang). */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().background(c.bg, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, text ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) c.surface else c.bg, RoundedCornerShape(11.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text, style = BioTheme.type.bodyBold, color = if (on) c.primary else c.muted)
            }
        }
    }
}

/** Pill filter kecil. */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) c.primary else c.surface, shape)
            .border(1.dp, if (selected) c.primary else c.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = BioTheme.type.label, color = if (selected) c.onPrimary else c.inkSoft)
    }
}

/** Kolom pencarian dengan ikon. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier.fillMaxWidth().background(c.surface, shape).border(1.dp, c.line, shape).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(BioIcons.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = BioTheme.type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = BioTheme.type.body, color = c.muted)
                    inner()
                }
            },
        )
    }
}
