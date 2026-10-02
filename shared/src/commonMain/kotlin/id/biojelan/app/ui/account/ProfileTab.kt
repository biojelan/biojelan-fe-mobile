package id.biojelan.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import id.biojelan.app.ui.components.Drip
import id.biojelan.app.ui.components.FadeInItem
import id.biojelan.app.ui.components.waveFill
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatOperatingHours
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.repository.AgenEdit
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.BioField
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.BioSwitch
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.FilterPill
import id.biojelan.app.ui.components.HairLine
import id.biojelan.app.ui.components.InfoItem
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.ProfileRow
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SectionHead
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import id.biojelan.app.ui.theme.ThemeController
import id.biojelan.app.data.local.LiveAlerts
import org.koin.compose.koinInject

private enum class ProfileSheet { None, Edit, Password, Delete }

/** Tab Profil untuk Klien maupun Agen (bagian Agen muncul jika `user.agen != null`). */
@Composable
fun ProfileTab(user: UserDto, account: AccountViewModel) {
    val c = BioTheme.colors
    val s = BioText.current
    var sheet by remember { mutableStateOf(ProfileSheet.None) }
    val busy by account.busy.collectAsStateWithLifecycle()
    val themeController: ThemeController = koinInject()
    val isDark by themeController.isDark.collectAsStateWithLifecycle()
    val liveAlerts: LiveAlerts = koinInject()
    val alertsOn by liveAlerts.enabled.collectAsStateWithLifecycle()
    val agen = user.agen
    val isDriver = user.roleId == AppConfig.ROLE_ID_DRIVER
    val roleLabel = when {
        isDriver -> s.roleDriver
        agen != null -> s.roleAgent
        else -> s.roleClient
    }

    val idLabel = when {
        isDriver -> s.labelIdDriver
        agen != null -> s.labelIdAgent
        else -> s.labelIdClient
    }
    val stats = if (agen != null) {
        listOf(
            HeroStat(s.tabStock, formatLiter(agen.stockLiter)),
            HeroStat(s.labelStatus, if (agen.isOpen) s.open else s.closed, dot = if (agen.isOpen) c.primary else c.rust),
            HeroStat(idLabel, agen.agenId.ifBlank { user.userId }, mono = true),
        )
    } else {
        emptyList()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ProfileHero(
            name = user.name,
            initials = initialsOf(user.name),
            roleLabel = roleLabel,
            stats = stats,
            onEdit = { account.clearFormError(); sheet = ProfileSheet.Edit },
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
        )
        if (stats.isNotEmpty()) Spacer(Modifier.height(HeroOverlap))
        Column(Modifier.padding(horizontal = ScreenPad)) {

            FadeInItem(1) {
                Column {
                    SectionHead(s.sectionAccountData)
                    Column(Modifier.fillMaxWidth().bioCard(16.dp)) {
                        InfoItem(BioIcons.Mail, s.fieldEmail, user.email.ifBlank { "-" })
                        HairLine()
                        InfoItem(BioIcons.Phone, s.labelPhone, user.phone.ifBlank { "-" })
                        if (agen == null) {
                            HairLine()
                            InfoItem(BioIcons.IdCard, idLabel, user.userId)
                        }
                    }
                }
            }

            if (agen != null) {
                FadeInItem(2) {
                    Column {
                        SectionHead(s.sectionAgentData)
                        Column(Modifier.fillMaxWidth().bioCard(16.dp)) {
                            InfoItem(BioIcons.Pin, s.labelAddress, agen.address.ifBlank { "-" })
                            HairLine()
                            InfoItem(BioIcons.Clock, s.labelOperatingHours, formatOperatingHours(agen.openAt, agen.closeAt, agen.openDays))
                            HairLine()
                            InfoItem(
                                BioIcons.Bank, s.labelBankAccount,
                                if (agen.bankName.isBlank() && agen.accountNumber.isBlank()) s.notFilled else "${agen.bankName} · ${agen.accountNumber}",
                            )
                            HairLine()
                            InfoItem(BioIcons.Drop, s.labelStockThreshold, formatLiter(AppConfig.STOCK_THRESHOLD_LITER) + " · " + s.setByKilang)
                        }
                    }
                }
            }

            FadeInItem(3) {
                Column {
                    SectionHead(s.sectionSettings)
                    Column(Modifier.fillMaxWidth().bioCard(16.dp)) {
                        ProfileRow(BioIcons.Edit, s.editProfile, onClick = { account.clearFormError(); sheet = ProfileSheet.Edit })
                        HairLine()
                        ProfileRow(BioIcons.Lock, s.changePassword, onClick = { account.clearFormError(); sheet = ProfileSheet.Password })
                        HairLine()
                        ProfileRow(
                            BioIcons.Moon, s.darkMode, value = s.darkModeHint,
                            onClick = { themeController.setDark(!isDark) },
                            trailing = { BioSwitch(checked = isDark, onCheckedChange = themeController::setDark) },
                        )
                        HairLine()
                        ProfileRow(
                            if (alertsOn) BioIcons.Bell else BioIcons.BellOff, s.liveAlertsTitle, value = s.liveAlertsHint,
                            onClick = liveAlerts::toggle,
                            trailing = { BioSwitch(checked = alertsOn, onCheckedChange = liveAlerts::setEnabled) },
                        )
                        HairLine()
                        ProfileRow(BioIcons.Logout, s.logout, tint = c.rust, onClick = { if (!busy) account.logout() })
                    }
                }
            }

            Text(
                s.deleteAccount,
                style = BioTheme.type.label,
                color = c.rust,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { account.clearFormError(); sheet = ProfileSheet.Delete }
                    .padding(12.dp),
            )
        }
    }

    when (sheet) {
        ProfileSheet.Edit -> EditProfileSheet(user, account) { sheet = ProfileSheet.None }
        ProfileSheet.Password -> ChangePasswordSheet(account) { sheet = ProfileSheet.None }
        ProfileSheet.Delete -> DeleteAccountSheet(account) { sheet = ProfileSheet.None }
        ProfileSheet.None -> Unit
    }
}


private val HeroOverlap = 34.dp

private data class HeroStat(val label: String, val value: String, val mono: Boolean = false, val dot: Color? = null)

/**
 * Banner hijau gradasi (satu bahasa visual dengan header Beranda gaya Bold): judul + tombol edit cepat,
 * avatar berring amber, nama, chip peran. Kartu ringkasan ([stats], hanya Agen) menempel di tepi bawahnya dan
 * menjorok keluar sebesar [HeroOverlap]; pemanggil wajib memberi spasi setinggi itu bila [stats] tidak kosong.
 */
@Composable
private fun ProfileHero(
    name: String,
    initials: String,
    roleLabel: String,
    stats: List<HeroStat>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val shape = RoundedCornerShape(28.dp)

    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.linearGradient(listOf(c.primary, c.primaryDeep)), shape),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                listOf(46f, 80f, 114f, 148f).forEach { r ->
                    drawCircle(
                        c.onPrimary.copy(alpha = 0.10f),
                        radius = r.dp.toPx(),
                        center = Offset(w * 0.98f, -h * 0.08f),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
                drawPath(waveFill(w, h, h * 0.82f, 4.dp.toPx(), 120.dp.toPx(), 0.8f), c.amber.copy(alpha = 0.09f))
                drawPath(waveFill(w, h, h * 0.90f, 3.dp.toPx(), 90.dp.toPx(), 2.6f), c.amber.copy(alpha = 0.13f))
                drawCircle(c.amber.copy(alpha = 0.55f), 3.dp.toPx(), Offset(w * 0.24f, h * 0.30f))
                drawCircle(c.amber.copy(alpha = 0.40f), 2.dp.toPx(), Offset(w * 0.17f, h * 0.42f))
            }

            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = if (stats.isEmpty()) 22.dp else 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.profileTitle, style = BioTheme.type.topTitle, color = c.onPrimary, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c.onPrimary.copy(alpha = 0.16f))
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(BioIcons.Edit, contentDescription = s.editProfile, tint = c.onPrimary, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .size(84.dp)
                        .background(c.onPrimary.copy(alpha = 0.16f), CircleShape)
                        .border(3.dp, c.amber, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, style = BioTheme.type.display, color = c.onPrimary)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    name,
                    style = BioTheme.type.headline,
                    color = c.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeroTag(roleLabel)
                }
            }
        }

        if (stats.isNotEmpty()) Row(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = HeroOverlap)
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(18.dp), clip = false)
                .bioCard(18.dp)
                .height(IntrinsicSize.Min),
        ) {
            stats.forEachIndexed { i, st ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp).background(c.line))
                Column(
                    Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(st.label, style = BioTheme.type.caption, color = c.muted, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        st.dot?.let { Drip(it, size = 8.dp) }
                        Text(
                            st.value,
                            style = if (st.mono) BioTheme.type.mono else BioTheme.type.statNumber,
                            color = st.dot ?: c.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Label kecil di atas banner hijau (BioChip bawaan berwarna terang, kurang cocok di atas hijau pekat). */
@Composable
private fun HeroTag(text: String) {
    val c = BioTheme.colors
    Row(
        Modifier.background(c.onPrimary.copy(alpha = 0.16f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text, style = BioTheme.type.chip, color = c.onPrimary, maxLines = 1)
    }
}

private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

@Composable
private fun EditProfileSheet(user: UserDto, account: AccountViewModel, onClose: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    val busy by account.busy.collectAsStateWithLifecycle()
    val serverError by account.formError.collectAsStateWithLifecycle()
    val agen = user.agen

    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var address by remember { mutableStateOf(agen?.address.orEmpty()) }
    var bank by remember { mutableStateOf(agen?.bankName.orEmpty()) }
    var accountNumber by remember { mutableStateOf(agen?.accountNumber.orEmpty()) }
    var openAt by remember { mutableStateOf(agen?.openAt.orEmpty()) }
    var closeAt by remember { mutableStateOf(agen?.closeAt.orEmpty()) }
    var days by remember { mutableStateOf(agen?.openDays?.map { it.lowercase() }.orEmpty()) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }

    BioSheet(s.editProfileTitle, onDismiss = onClose) {
        BioField(s.fieldName, name, { name = it }, error = errors["name"])
        BioField(s.fieldPhone, phone, { phone = it }, keyboardType = KeyboardType.Phone, placeholder = s.placeholderPhone)

        if (agen != null) {
            BioField(s.fieldAddress, address, { address = it }, placeholder = s.placeholderAddress)
            BioField(s.fieldBankName, bank, { bank = it }, placeholder = s.placeholderBankName)
            BioField(s.fieldAccountNumber, accountNumber, { accountNumber = it }, keyboardType = KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BioField(s.fieldOpenTime, openAt, { openAt = it }, Modifier.weight(1f), placeholder = s.placeholderOpenTime, error = errors["openAt"], keyboardType = KeyboardType.Number)
                BioField(s.fieldCloseTime, closeAt, { closeAt = it }, Modifier.weight(1f), placeholder = s.placeholderCloseTime, error = errors["closeAt"], keyboardType = KeyboardType.Number)
            }
            Text(s.fieldOpenDays, style = BioTheme.type.label, color = c.inkSoft, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppConfig.WEEK_DAYS.forEach { day ->
                    FilterPill(
                        text = day.replaceFirstChar { it.uppercase() },
                        selected = day in days,
                        onClick = { days = if (day in days) days - day else days + day },
                    )
                }
            }
        }

        serverError?.let { NoteBox(it, tone = NoteTone.Rust, icon = BioIcons.Alert, modifier = Modifier.padding(bottom = 12.dp)) }

        BioButton(
            s.saveChanges,
            onClick = {
                val found = buildMap {
                    if (name.isBlank()) put("name", s.errorNameRequired)
                    if (agen != null) {
                        if (openAt.isNotBlank() && !TIME_REGEX.matches(openAt.trim())) put("openAt", s.errorTimeFormat)
                        if (closeAt.isNotBlank() && !TIME_REGEX.matches(closeAt.trim())) put("closeAt", s.errorTimeFormat)
                    }
                }
                errors = found
                if (found.isEmpty()) {
                    val edit = if (agen != null) AgenEdit(
                        address = address.trim(),
                        bankName = bank.trim(),
                        accountNumber = accountNumber.trim(),
                        openAt = openAt.trim(),
                        closeAt = closeAt.trim(),
                        openDays = AppConfig.WEEK_DAYS.filter { it in days },
                    ) else null
                    account.updateProfile(name, phone, edit, onSuccess = onClose)
                }
            },
            loading = busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChangePasswordSheet(account: AccountViewModel, onClose: () -> Unit) {
    val s = BioText.current
    val busy by account.busy.collectAsStateWithLifecycle()
    val serverError by account.formError.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }

    fun submit() {
        val found = buildMap {
            if (current.isEmpty()) put("current", s.errorCurrentPasswordRequired)
            if (next.length < 8) put("next", s.errorMinChars)
            if (confirm != next) put("confirm", s.errorConfirmMismatch)
        }
        errors = found
        if (found.isEmpty()) account.changePassword(current, next, confirm, onSuccess = onClose)
    }

    BioSheet(s.changePasswordTitle, onDismiss = onClose) {
        BioField(s.fieldCurrentPassword, current, { current = it }, isPassword = true, keyboardType = KeyboardType.Password, error = errors["current"])
        BioField(s.fieldNewPassword, next, { next = it }, isPassword = true, keyboardType = KeyboardType.Password, hint = s.hintMinChars, error = errors["next"])
        BioField(
            s.fieldRepeatNewPassword, confirm, { confirm = it },
            isPassword = true, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
            onDone = { submit() }, error = errors["confirm"],
        )
        serverError?.let { NoteBox(it, tone = NoteTone.Rust, icon = BioIcons.Alert, modifier = Modifier.padding(bottom = 12.dp)) }
        BioButton(s.savePassword, onClick = { submit() }, loading = busy, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DeleteAccountSheet(account: AccountViewModel, onClose: () -> Unit) {
    val s = BioText.current
    val busy by account.busy.collectAsStateWithLifecycle()
    val serverError by account.formError.collectAsStateWithLifecycle()
    var typed by remember { mutableStateOf("") }

    BioSheet(s.deleteAccountTitle, onDismiss = onClose) {
        NoteBox(
            s.deleteAccountWarning,
            tone = NoteTone.Rust,
            icon = BioIcons.Alert,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        BioField(s.deleteAccountConfirmLabel, typed, { typed = it }, placeholder = s.deleteAccountConfirmWord)
        serverError?.let { NoteBox(it, tone = NoteTone.Rust, icon = BioIcons.Alert, modifier = Modifier.padding(bottom = 12.dp)) }
        BioButton(
            s.deleteMyAccount,
            onClick = { account.deleteAccount() },
            style = BtnStyle.Rust,
            enabled = typed.trim() == s.deleteAccountConfirmWord,
            loading = busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}