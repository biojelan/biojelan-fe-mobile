package id.biojelan.app.core

/** Epoch millis sekarang (UTC). */
internal expect fun nowEpochMillis(): Long

/** Selisih zona waktu perangkat terhadap UTC saat ini, dalam detik (WIB = 25200). */
internal expect fun localUtcOffsetSeconds(): Int
