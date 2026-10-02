package id.biojelan.app.data.local

import com.russhwolf.settings.Settings

/**
 * Penyimpanan sesi di perangkat (SharedPreferences di Android, NSUserDefaults di iOS).
 *
 * CATATAN KEAMANAN: token disimpan tanpa enkripsi. Untuk rilis produksi sebaiknya pindah ke
 * Android Keystore / iOS Keychain (mis. lewat multiplatform-settings `KeychainSettings` + EncryptedSharedPreferences).
 */
class SessionStore(private val settings: Settings) {

    var token: String?
        get() = settings.getStringOrNull(KEY_TOKEN)
        set(value) {
            if (value == null) settings.remove(KEY_TOKEN) else settings.putString(KEY_TOKEN, value)
        }

    /** Salinan terakhir profil (JSON) — dipakai agar app tetap terbuka saat start dalam kondisi offline. */
    var cachedUserJson: String?
        get() = settings.getStringOrNull(KEY_USER)
        set(value) {
            if (value == null) settings.remove(KEY_USER) else settings.putString(KEY_USER, value)
        }

    /** Preferensi tema: "light" (default) atau "dark". Nilai lain dianggap "light". */
    var themeMode: String?
        get() = settings.getStringOrNull(KEY_THEME)
        set(value) {
            if (value == null) settings.remove(KEY_THEME) else settings.putString(KEY_THEME, value)
        }

    /** Kode bahasa: "id" (default) atau "en". */
    var language: String?
        get() = settings.getStringOrNull(KEY_LANGUAGE)
        set(value) {
            if (value == null) settings.remove(KEY_LANGUAGE) else settings.putString(KEY_LANGUAGE, value)
        }

    /** Notifikasi transaksi masuk (toast saat polling menemukan yang baru). Null = belum diatur (default aktif). */
    var liveAlerts: Boolean?
        get() = settings.getBooleanOrNull(KEY_LIVE_ALERTS)
        set(value) {
            if (value == null) settings.remove(KEY_LIVE_ALERTS) else settings.putBoolean(KEY_LIVE_ALERTS, value)
        }

    /** Riwayat notifikasi (JSON). Milik pengguna yang sedang login, jadi ikut dihapus saat logout. */
    var alertsJson: String?
        get() = settings.getStringOrNull(KEY_ALERTS)
        set(value) {
            if (value == null) settings.remove(KEY_ALERTS) else settings.putString(KEY_ALERTS, value)
        }

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER)
        settings.remove(KEY_ALERTS)
        // Theme & language preferences intentionally NOT cleared on logout
    }

    private companion object {
        const val KEY_TOKEN = "session.token"
        const val KEY_USER = "session.user"
        const val KEY_THEME = "pref.theme"
        const val KEY_LANGUAGE = "pref.language"
        const val KEY_LIVE_ALERTS = "pref.live_alerts"
        const val KEY_ALERTS = "session.alerts"
    }
}
