package id.biojelan.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Durasi minimum indikator "sedang memuat" tampil, supaya request yang cepat tetap terlihat berjalan. */
const val MIN_REFRESH_FEEDBACK_MS = 700L

/**
 * Mengembalikan true selama [loading] true, dan menahannya tetap true minimal [minMs] setelah loading
 * selesai. Tanpa ini, refresh yang selesai dalam beberapa ratus ms membuat tombol terasa tidak melakukan apa-apa.
 */
@Composable
fun rememberHeldLoading(loading: Boolean, minMs: Long = MIN_REFRESH_FEEDBACK_MS): Boolean {
    var held by remember { mutableStateOf(loading) }
    LaunchedEffect(loading) {
        if (loading) {
            held = true
        } else if (held) {
            delay(minMs)
            held = false
        }
    }
    return held
}
