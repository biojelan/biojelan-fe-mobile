package id.biojelan.app.data.mock

import id.biojelan.app.core.AppConfig
import id.biojelan.app.data.remote.AgenDto
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.remote.AuthPayload
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.remote.UserDto

/**
 * Data dummy realistis sesuai API-DOC.
 *
 * Dipakai oleh [MockApiClient] sebagai fallback saat backend belum di-deploy.
 * Tiga akun tersedia:
 * - Klien:  `klien@biojelan.id`  / `password123`
 * - Agen:   `agen@biojelan.id`   / `password123`
 * - Driver: `driver@biojelan.id` / `password123`
 */
object MockData {

    // ========================= Credentials & Tokens =========================

    const val MOCK_PASSWORD = "password123"
    const val TOKEN_KLIEN = "mock-token-klien-001"
    const val TOKEN_AGEN = "mock-token-agen-001"
    const val TOKEN_DRIVER = "mock-token-driver-001"

    /** email → token */
    val credentials = mapOf(
        "klien@biojelan.id" to TOKEN_KLIEN,
        "agen@biojelan.id" to TOKEN_AGEN,
        "driver@biojelan.id" to TOKEN_DRIVER,
    )

    // ========================= Users =========================

    val klienUser = UserDto(
        userId = "1",
        roleId = 7,
        name = "Budi Santoso",
        email = "klien@biojelan.id",
        phone = "081234567890",
        isVerified = true,
        isActive = true,
        agen = null, // Klien → tidak ada objek agen
    )

    val agenUser = UserDto(
        userId = "2",
        roleId = 6,
        name = "Siti Nurhaliza",
        email = "agen@biojelan.id",
        phone = "081298765432",
        isVerified = true,
        isActive = true,
        agen = AgenDto(
            agenId = "101",
            address = "Jl. Merdeka No. 10, Cikini, Jakarta Pusat",
            latitude = -6.1862,
            longitude = 106.8399,
            bankName = "BCA",
            accountNumber = "1234567890",
            openAt = "08:00",
            closeAt = "17:00",
            openDays = listOf("senin", "selasa", "rabu", "kamis", "jumat"),
            isOpen = true,
            stockLiter = 120.5,
        ),
    )

    /** Driver (role_id 5): tidak punya objek `agen`; dikenali dari `role_id`. */
    val driverUser = UserDto(
        userId = "3",
        roleId = 5,
        name = "Rudi Hartono",
        email = "driver@biojelan.id",
        phone = "081311112222",
        isVerified = true,
        isActive = true,
        agen = null,
    )

    /** token → UserDto */
    val userByToken = mapOf(
        TOKEN_KLIEN to klienUser,
        TOKEN_AGEN to agenUser,
        TOKEN_DRIVER to driverUser,
    )

    // ========================= Auth Payloads =========================

    fun authPayloadForEmail(email: String): AuthPayload? {
        val token = credentials[email] ?: return null
        val user = userByToken[token] ?: return null
        return AuthPayload(token = token, name = user.name, email = user.email)
    }

    /** Register selalu sukses, anggap jadi Klien baru. */
    fun registerPayload(name: String, email: String): AuthPayload =
        AuthPayload(token = TOKEN_KLIEN, name = name, email = email)

    // ========================= Agen List =========================

    val agenList = listOf(
        AgenSummaryDto(
            agenId = "101",
            roleId = 6,
            name = "Siti Nurhaliza",
            phone = "081298765432",
            address = "Jl. Merdeka No. 10, Cikini, Jakarta Pusat",
            latitude = -6.1862,
            longitude = 106.8399,
            openAt = "08:00",
            closeAt = "17:00",
            isOpen = true,
            openDays = listOf("senin", "selasa", "rabu", "kamis", "jumat"),
        ),
        AgenSummaryDto(
            agenId = "102",
            roleId = 6,
            name = "Ahmad Rizky",
            phone = "081377889900",
            address = "Jl. Sudirman No. 25, Kebayoran Baru, Jakarta Selatan",
            latitude = -6.2297,
            longitude = 106.8083,
            openAt = "09:00",
            closeAt = "18:00",
            isOpen = true,
            openDays = listOf("senin", "selasa", "rabu", "kamis", "jumat", "sabtu"),
        ),
        AgenSummaryDto(
            agenId = "103",
            roleId = 6,
            name = "Dewi Lestari",
            phone = "081511223344",
            address = "Jl. Gajah Mada No. 5, Glodok, Jakarta Barat",
            latitude = -6.1490,
            longitude = 106.8171,
            openAt = "07:30",
            closeAt = "16:30",
            isOpen = false,
            openDays = listOf("senin", "rabu", "jumat"),
        ),
        AgenSummaryDto(
            agenId = "104",
            roleId = 6,
            name = "Rudi Hermawan",
            phone = "081644556677",
            address = "Jl. Pemuda No. 8, Pulo Gadung, Jakarta Timur",
            latitude = -6.1835,
            longitude = 106.8990,
            openAt = "08:00",
            closeAt = "15:00",
            isOpen = true,
            openDays = listOf("selasa", "kamis", "sabtu"),
        ),
    )

    // ========================= Transactions =========================

    val klienTransactions = listOf(
        TransactionDto(
            transactionId = "TXN-001",
            agenId = "101",
            klienId = "1",
            klienName = "Budi Santoso",
            agenName = "Siti Nurhaliza",
            volumeLiter = 5.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 5 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "ACCEPTED",
            createdAt = "2026-09-20 10:30:00",
            updatedAt = "2026-09-20 11:00:00",
        ),
        TransactionDto(
            transactionId = "TXN-002",
            agenId = "102",
            klienId = "1",
            klienName = "Budi Santoso",
            agenName = "Ahmad Rizky",
            volumeLiter = 3.5,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = (3.5 * AppConfig.DEFAULT_PRICE_PER_LITER).toLong(),
            status = "PENDING",
            createdAt = "2026-09-21 14:15:00",
            updatedAt = "2026-09-21 14:15:00",
        ),
        TransactionDto(
            transactionId = "TXN-003",
            agenId = "103",
            klienId = "1",
            klienName = "Budi Santoso",
            agenName = "Dewi Lestari",
            volumeLiter = 2.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 2 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "CANCELLED",
            createdAt = "2026-09-19 09:00:00",
            updatedAt = "2026-09-19 09:30:00",
        ),
        TransactionDto(
            transactionId = "TXN-006",
            agenId = "101",
            klienId = "1",
            klienName = "Budi Santoso",
            agenName = "Siti Nurhaliza",
            volumeLiter = 4.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 4 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "CANCEL_REQUESTED",
            createdAt = "2026-09-22 13:00:00",
            updatedAt = "2026-09-22 13:20:00",
        ),
    )

    val agenTransactions = listOf(
        TransactionDto(
            transactionId = "TXN-001",
            agenId = "101",
            klienId = "1",
            klienName = "Budi Santoso",
            agenName = "Siti Nurhaliza",
            volumeLiter = 5.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 5 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "ACCEPTED",
            createdAt = "2026-09-20 10:30:00",
            updatedAt = "2026-09-20 11:00:00",
        ),
        TransactionDto(
            transactionId = "TXN-004",
            agenId = "101",
            klienId = "3",
            klienName = "Rina Wulandari",
            agenName = "Siti Nurhaliza",
            volumeLiter = 8.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 8 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "PENDING",
            createdAt = "2026-09-22 08:45:00",
            updatedAt = "2026-09-22 08:45:00",
        ),
        TransactionDto(
            transactionId = "TXN-005",
            agenId = "101",
            klienId = "4",
            klienName = "Hendra Wijaya",
            agenName = "Siti Nurhaliza",
            volumeLiter = 10.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 10 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "ACCEPTED",
            createdAt = "2026-09-18 16:00:00",
            updatedAt = "2026-09-18 16:30:00",
        ),
        TransactionDto(
            transactionId = "TXN-006",
            agenId = "101",
            klienId = "1",
            klienName = "Budi Santoso",
            agenName = "Siti Nurhaliza",
            volumeLiter = 4.0,
            price = AppConfig.DEFAULT_PRICE_PER_LITER,
            totalPrice = 4 * AppConfig.DEFAULT_PRICE_PER_LITER,
            status = "CANCEL_REQUESTED",
            createdAt = "2026-09-22 13:00:00",
            updatedAt = "2026-09-22 13:20:00",
        ),
    )

    // ========================= Driver → Agen =========================

    /** Harga per liter Agen → Kilang di mock (di backend dihitung server). */
    const val DRIVER_PRICE_PER_LITER = 8_000L

    /** Kondisi awal transaksi Driver → Agen; [MockApiClient] menyalinnya ke daftar yang bisa berubah. */
    val driverTransactions = listOf(
        DriverTransactionDto(
            transactionId = "TXN-D003",
            driverId = "3",
            agenId = "101",
            agenName = "Siti Nurhaliza",
            volumeLiter = 25.0,
            price = DRIVER_PRICE_PER_LITER,
            totalPrice = 25 * DRIVER_PRICE_PER_LITER,
            status = "CANCEL_REQUESTED",
            transactionNote = "Salah catat volume",
            createdAt = "2026-09-27 15:00:00",
            updatedAt = "2026-09-27 15:30:00",
        ),
        DriverTransactionDto(
            transactionId = "TXN-D002",
            driverId = "3",
            agenId = "101",
            agenName = "Siti Nurhaliza",
            volumeLiter = 30.0,
            price = DRIVER_PRICE_PER_LITER,
            totalPrice = 30 * DRIVER_PRICE_PER_LITER,
            status = "PENDING",
            transactionNote = "Diambil sore ini",
            createdAt = "2026-09-27 10:00:00",
            updatedAt = "2026-09-27 10:00:00",
        ),
        DriverTransactionDto(
            transactionId = "TXN-D001",
            driverId = "3",
            agenId = "101",
            agenName = "Siti Nurhaliza",
            volumeLiter = 45.0,
            price = DRIVER_PRICE_PER_LITER,
            totalPrice = 45 * DRIVER_PRICE_PER_LITER,
            status = "ACCEPTED",
            createdAt = "2026-09-20 09:00:00",
            updatedAt = "2026-09-20 09:20:00",
        ),
    )

    // ========================= Pickup (Driver) =========================

    /** Kondisi awal penjemputan; [MockApiClient] menyalinnya ke state yang bisa diubah Driver. */
    val agenPickupStatus = PickupStatusDto(
        pickupId = "pkp-mock-001",
        driverId = "3",
        agenId = "101",
        date = "2026-09-28",
        status = "ASSIGNED",
        updatedAt = "2026-09-28 08:00:00",
    )
}
