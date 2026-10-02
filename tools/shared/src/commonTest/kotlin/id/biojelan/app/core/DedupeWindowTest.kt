package id.biojelan.app.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DedupeWindowTest {
    private var t = 0L
    private val window = DedupeWindow(windowMs = 2_000L, now = { t })

    @Test
    fun first_message_is_emitted() {
        assertTrue(window.shouldEmit("Gagal"))
    }

    @Test
    fun same_message_inside_window_is_suppressed() {
        window.shouldEmit("Gagal")
        t = 1_999
        assertFalse(window.shouldEmit("Gagal"))
    }

    @Test
    fun same_message_after_window_is_emitted_again() {
        window.shouldEmit("Gagal")
        t = 2_000
        assertTrue(window.shouldEmit("Gagal"))
    }

    @Test
    fun different_message_inside_window_is_emitted() {
        window.shouldEmit("Gagal")
        t = 100
        assertTrue(window.shouldEmit("Sesi berakhir"))
    }

    @Test
    fun suppressed_repeats_do_not_extend_the_window() {
        window.shouldEmit("Gagal")
        t = 1_500
        assertFalse(window.shouldEmit("Gagal"))
        t = 2_100 // 2,1 dtk sejak yang TERAKHIR TAMPIL, walau hanya 0,6 dtk sejak yang ditekan
        assertTrue(window.shouldEmit("Gagal"))
    }

    @Test
    fun clock_moving_backwards_does_not_suppress_messages() {
        t = 100_000
        window.shouldEmit("Gagal")
        t = 50_000
        assertTrue(window.shouldEmit("Gagal"))
    }
}
