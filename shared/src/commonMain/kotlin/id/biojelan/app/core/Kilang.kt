package id.biojelan.app.core

/** Lokasi Kilang: titik awal dan akhir rute Driver. Koordinat 0,0 dan alamat kosong berarti belum diketahui. */
data class KilangPoint(val latitude: Double = 0.0, val longitude: Double = 0.0, val address: String = "") {
    val hasCoordinates: Boolean get() = latitude != 0.0 || longitude != 0.0
    val hasLocation: Boolean get() = hasCoordinates || address.isNotBlank()
}
