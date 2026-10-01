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
     * Aktifkan mode fallback: API call yang gagal karena JARINGAN (timeout, tidak bisa connect) otomatis
     * dijawab mock, supaya app tetap bisa dipakai meski backend tidak terjangkau. Kegagalan karena SERVER
     * (401/422/500 dsb.) tetap ditampilkan apa adanya, bukan fallback.
     *
     * Set ke `false` begitu backend sudah live dan stabil (atau untuk debugging integrasi backend).
     */
    const val ENABLE_FALLBACK = true

    /** `role_id` Driver di backend (RoleSeeder: 1-4 kilang, 5 driver, 6 agent, 7 client). */
    const val ROLE_ID_DRIVER = 5

    /**
     * Transaksi Driver↔Agen dan Pickup sudah live semua di backend dan sudah dicocokkan field-by-field
     * (lihat `DriverTransactionRepository` & `PickupRepository`), jadi tidak ada lagi jalur yang dipaksa
     * mock di luar [ENABLE_FALLBACK] biasa. Dipertahankan sebagai saklar darurat: set `true` untuk memaksa
     * endpoint pickup balik ke mock tanpa perlu mengubah kode lain, kalau ternyata ada sesuatu yang belum
     * cocok pas dites langsung ke server.
     */
    const val MOCK_PICKUP_API = false

    /** Mode tema default: "system" (ikut OS), "light", atau "dark". */
    const val DEFAULT_THEME = "system"

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
