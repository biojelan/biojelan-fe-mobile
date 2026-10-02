package id.biojelan.app.core

/**
 * Konfigurasi aplikasi. Semua angka bisnis yang BELUM punya endpoint di API-DOC
 * dikumpulkan di sini supaya gampang diganti begitu backend menyediakannya.
 */
object AppConfig {
    /**
     * Base URL backend asli (bukan `http://biojelan.id` yang ditulis di API-DOC — itu cuma placeholder
     * di dokumen, bukan server yang benar-benar hidup). Tanpa trailing slash: `ApiClient` menempelkan
     * path yang sudah diawali "/" (mis. "/api/login"), jadi slash ganda kalau ditambah di sini.
     */
    const val BASE_URL = "https://biojelan.callmeoda.web.id"

    const val CONNECT_TIMEOUT_MS = 10_000L
    const val REQUEST_TIMEOUT_MS = 20_000L

    /**
     * Jeda antar auto-refresh berkala (data transaksi/approval) selama layar aktif di depan. Polling otomatis
     * berhenti saat app di background dan langsung memuat ulang begitu app dibuka lagi.
     */
    const val AUTO_REFRESH_INTERVAL_MS = 15_000L

    /** Jarak minimum antar dua auto-refresh, supaya buka-tutup app berulang tidak menembak server terus. */
    const val AUTO_REFRESH_MIN_GAP_MS = 5_000L

    /** `role_id` Driver di backend (RoleSeeder: 1-4 kilang, 5 driver, 6 agent, 7 client). */
    const val ROLE_ID_DRIVER = 5

    /**
     * Email khusus yang dikenali backend sebagai transaksi tamu (`client_id` = null). Backend tidak punya
     * field nama/telepon tamu, jadi identitas tamu dititipkan di `transaction_note`.
     */
    const val GUEST_CLIENT_EMAIL = "guest.client@biojelan.id"

    /** Mode tema default: "light". Gelap hanya aktif kalau pengguna menyalakan switch di Profil. */
    const val DEFAULT_THEME = "light"

    /** Kode bahasa default: "id" (Indonesia) atau "en" (English). */
    const val DEFAULT_LANGUAGE = "id"

    /**
     * TODO(backend): belum ada endpoint harga. Prototype menampilkan "harga aktif dari Kilang".
     * Sampai ada, dipakai nilai default ini (lihat [id.biojelan.app.data.repository.PriceProvider]).
     */
    const val DEFAULT_PRICE_PER_LITER = 6_500L

    /**
     * TODO(backend): ambang stok Agen dikonfigurasi Superadmin di Kilang (default 500 L di prototype).
     * Belum ada endpoint konfigurasi, jadi sementara konstanta.
     */
    const val STOCK_THRESHOLD_LITER = 500.0

    /** Kode hari yang dipakai API untuk `open_day` / `open_days`, urut Senin–Minggu. */
    val WEEK_DAYS = listOf("senin", "selasa", "rabu", "kamis", "jumat", "sabtu", "minggu")
}
