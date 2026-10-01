package id.biojelan.app.core

/**
 * URL Google Maps mode "dir" (arah/direction) — begitu dibuka, Maps langsung mulai rute dari lokasi
 * pengguna saat ini ke [lat]/[lng] (auto-direction), bukan cuma nampilin pin seperti mode "search".
 *
 * App ini tidak melacak lokasi Driver secara real-time (checkpoint-based, seperti sistem resi paket):
 * status transaksi/pickup di-update manual tiap tahap (mis. Agen menerima transaksi, Driver menandai
 * pickup selesai), bukan lewat GPS berkelanjutan. Tombol rute ini cuma jembatan ke app Maps eksternal
 * untuk navigasi; pelacakan posisi sepenuhnya ditangani Google Maps, bukan oleh app ini.
 */
fun googleMapsDirectionsUrl(lat: Double, lng: Double, address: String): String {
    val destination = if (lat != 0.0 || lng != 0.0) "$lat,$lng" else address.trim().replace(' ', '+')
    return "https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=driving"
}
