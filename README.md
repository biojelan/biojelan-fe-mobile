# BioJelan Mobile

Aplikasi mobile BioJelan untuk transaksi jual-beli minyak jelantah, menghubungkan tiga peran pengguna:
Klien (penjual), Agen (pengepul), dan Driver (pengambil dari Kilang).

Repo ini isinya khusus aplikasi mobile (Kotlin Multiplatform). Dua bagian lain proyek ada di repo terpisah:

- **Requirement** — dokumentasi API, prototype tampilan, dan changelog kebutuhan (folder `API-DOC/` di
  repo ini adalah salinan untuk referensi, bukan sumber utama).
- **BE Dashboard** — backend Laravel (`biojelan-be-dashboard`) beserta dashboard webnya.

## Tech Stack

- **Kotlin Multiplatform** + **Compose Multiplatform** — satu basis kode UI untuk Android & iOS
- **Ktor Client** — komunikasi jaringan
- **Kotlinx Serialization** — parsing JSON
- **Koin** — dependency injection
- **Backend**: REST API (Laravel)

## Project Structure

```
shared/      Modul KMP bersama: UI, ViewModel, jaringan, sesi (dipakai Android & iOS)
androidApp/  Host aplikasi Android
iosApp/      Host aplikasi iOS (SwiftUI + ComposeUIViewController)
API-DOC/     Dokumentasi kontrak API
```

## Setup

**Android** — buka folder ini di Android Studio, jalankan konfigurasi `androidApp`.

**iOS** — buka `iosApp/iosApp.xcodeproj` di Xcode. Isi `TEAM_ID` di
`iosApp/Configuration/Config.xcconfig` untuk menjalankan di device fisik.

Konfigurasi seperti base URL API ada di `shared/src/commonMain/kotlin/id/biojelan/app/core/AppConfig.kt`.

## Role & Feature

| Peran | Fitur utama |
|---|---|
| Klien | Cari Agen, jual minyak jelantah, riwayat & konfirmasi transaksi |
| Agen | Kelola toko & stok, terima transaksi dari Klien, kelola pengambilan oleh Driver |
| Driver | Catat pengambilan dari Agen, rute ke lokasi Agen (Google Maps), pantau status penjemputan |

## Font & Licenses

Sora, Manrope, IBM Plex Mono (SIL Open Font License 1.1) — lihat `licenses/fonts/`.
