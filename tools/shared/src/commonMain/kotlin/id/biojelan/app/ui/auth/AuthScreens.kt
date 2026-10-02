package id.biojelan.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioField
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SegmentedTabs
import id.biojelan.app.ui.components.SubHeader
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
internal object LoginRoute

@Serializable
internal object ForgotRoute

/** Alur untuk pengguna yang belum masuk: Masuk/Daftar -> Lupa kata sandi. */
@Composable
fun AuthFlow() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(onForgot = { nav.navigate(ForgotRoute) })
        }
        composable<ForgotRoute> {
            ForgotPasswordScreen(onBack = { nav.popBackStack() })
        }
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Int = 56) {
    val c = BioTheme.colors
    Box(
        modifier = modifier
            .size(size.dp)
            .background(Brush.linearGradient(listOf(Color(0xFFE9BA55), c.amberDeep)), RoundedCornerShape((size * 0.3f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(BioIcons.DropFilled, contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun LoginScreen(onForgot: () -> Unit, vm: AuthViewModel = koinViewModel()) {
    val c = BioTheme.colors
    val s = BioText.current
    val state by vm.state.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(0) } // 0 = masuk, 1 = daftar
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val isRegister = mode == 1

    fun submit() {
        if (isRegister) vm.register(name, email, password, confirmation) else vm.login(email, password)
    }

    Box(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPad, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            BrandMark()
            Spacer(Modifier.height(18.dp))
            Text(
                if (isRegister) s.createAccountTitle else s.welcomeTitle,
                style = BioTheme.type.title,
                color = c.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isRegister) s.createAccountSubtitle else s.welcomeSubtitle,
                style = BioTheme.type.body,
                color = c.muted,
            )
            Spacer(Modifier.height(22.dp))

            SegmentedTabs(
                options = listOf(s.signIn, s.signUp),
                selected = mode,
                onSelect = {
                    mode = it
                    vm.clearErrors()
                },
            )
            Spacer(Modifier.height(20.dp))

            if (isRegister) {
                BioField(s.fieldFullName, name, { name = it }, placeholder = s.placeholderName, error = state.fieldErrors["name"])
            }
            BioField(
                s.fieldEmail, email, { email = it },
                placeholder = s.placeholderEmail,
                keyboardType = KeyboardType.Email,
                error = state.fieldErrors["email"],
            )
            BioField(
                s.fieldPassword, password, { password = it },
                placeholder = if (isRegister) s.placeholderPasswordMin else s.placeholderPasswordYours,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = if (isRegister) ImeAction.Next else ImeAction.Done,
                onDone = { submit() },
                error = state.fieldErrors["password"],
            )
            if (isRegister) {
                BioField(
                    s.fieldRepeatPassword, confirmation, { confirmation = it },
                    placeholder = s.placeholderRepeatPassword,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onDone = { submit() },
                    error = state.fieldErrors["confirmation"],
                )
            }

            state.error?.let {
                NoteBox(it, tone = NoteTone.Rust, icon = BioIcons.Alert, modifier = Modifier.padding(bottom = 14.dp))
            }

            BioButton(
                text = if (isRegister) s.signUp else s.signIn,
                onClick = { submit() },
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!isRegister) {
                Text(
                    s.forgotPasswordLink,
                    style = BioTheme.type.label,
                    color = c.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            vm.clearErrors()
                            onForgot()
                        }
                        .padding(12.dp),
                )
            }

            Spacer(Modifier.height(if (isRegister) 22.dp else 8.dp))
            GuestEntryButton(onClick = vm::enterGuestMode)
        }
    }
}

/**
 * Tombol "Lihat sebagai tamu" — dibuat menonjol (bukan teks kecil abu-abu) supaya jelas
 * ini jalur masuk yang valid, sejalan dengan revisi prototype HTML.
 */
@Composable
private fun GuestEntryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.primaryTint)
            .border(1.5.dp, c.primaryTint, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BioIcons.Eye, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            s.guestEntry,
            style = BioTheme.type.label,
            color = c.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, vm: AuthViewModel = koinViewModel()) {
    val c = BioTheme.colors
    val s = BioText.current
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SubHeader(s.forgotPasswordTitle, onBack)
            Column(Modifier.padding(horizontal = ScreenPad, vertical = 12.dp)) {
                Text(s.forgotPasswordHeadline, style = BioTheme.type.headline, color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    s.forgotPasswordBody,
                    style = BioTheme.type.body,
                    color = c.muted,
                )
                Spacer(Modifier.height(22.dp))
                BioField(
                    s.fieldEmail, email, { email = it },
                    placeholder = s.placeholderEmail,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onDone = { vm.forgotPassword(email) },
                    error = state.fieldErrors["email"],
                )
                state.error?.let {
                    NoteBox(it, tone = NoteTone.Rust, icon = BioIcons.Alert, modifier = Modifier.padding(bottom = 14.dp))
                }
                if (state.resetSent) {
                    NoteBox(
                        s.resetSentNote,
                        icon = BioIcons.Mail,
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }
                BioButton(
                    s.sendResetLink,
                    onClick = { vm.forgotPassword(email) },
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SplashScreen(error: String? = null, onRetry: (() -> Unit)? = null) {
    val c = BioTheme.colors
    val s = BioText.current
    Box(Modifier.fillMaxSize().background(c.paper).systemBarsPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            BrandMark(size = 72)
            Spacer(Modifier.height(16.dp))
            Text(s.appName, style = BioTheme.type.title, color = c.ink)
            Spacer(Modifier.height(20.dp))
            if (error == null) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            } else {
                Text(error, style = BioTheme.type.body, color = c.muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                if (onRetry != null) BioButton(s.retryButton, onRetry, icon = BioIcons.Refresh)
            }
        }
    }
}
