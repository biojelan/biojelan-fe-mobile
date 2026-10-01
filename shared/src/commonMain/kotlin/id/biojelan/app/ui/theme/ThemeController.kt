package id.biojelan.app.ui.theme

import id.biojelan.app.data.local.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pemegang state tema yang bisa diamati UI. Default TERANG; gelap hanya aktif kalau pengguna
 * menyalakannya sendiri lewat switch di tab Profil (tidak lagi otomatis mengikuti OS).
 * Pilihan disimpan di [SessionStore] sehingga bertahan setelah app ditutup & setelah logout.
 */
class ThemeController(private val store: SessionStore) {
    private val _isDark = MutableStateFlow(store.themeMode == MODE_DARK)

    /** `true` = mode gelap. */
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    fun setDark(enabled: Boolean) {
        store.themeMode = if (enabled) MODE_DARK else MODE_LIGHT
        _isDark.value = enabled
    }

    private companion object {
        const val MODE_DARK = "dark"
        const val MODE_LIGHT = "light"
    }
}
