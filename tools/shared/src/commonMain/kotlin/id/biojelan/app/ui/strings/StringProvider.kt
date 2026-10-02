package id.biojelan.app.ui.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal untuk string saat ini. Default: Bahasa Indonesia.
 *
 * Di-provide di `BioTheme()`, dipilih berdasarkan preferensi pengguna di [SessionStore].
 *
 * Akses cepat: `BioText.current.welcomeTitle`
 */
val LocalBioStrings = staticCompositionLocalOf<BioStrings> { IdStrings }

/** Shortcut untuk mengakses [BioStrings] dari Composable. */
object BioText {
    val current: BioStrings
        @Composable @ReadOnlyComposable get() = LocalBioStrings.current
}

/** Mapping kode bahasa → implementasi. */
fun bioStringsFor(languageCode: String): BioStrings = when (languageCode.lowercase()) {
    "en" -> EnStrings
    else -> IdStrings
}
