package id.biojelan.app.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Perilaku mesin polling berkala. Scope memakai Dispatchers.Unconfined: coroutine jalan sinkron sampai
 * suspend pertamanya, jadi urutan kejadian deterministik tanpa delay nyata.
 */
class AutoRefreshGateTest {
    private class Clock(var t: Long = 0L) { fun now() = t }

    private class Harness(
        val minGap: Long = 5_000L,
        var allowed: Boolean = true,
        val block: suspend (Harness) -> Unit = {},
    ) {
        val clock = Clock()
        val leaked = mutableListOf<Throwable>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, e -> leaked += e })
        var started = 0
        var finished = 0
        var cancelled = 0
        val gate = AutoRefreshGate(
            scope = scope,
            minGapMs = minGap,
            now = clock::now,
            canRun = { allowed },
            work = {
                started++
                try {
                    block(this)
                    finished++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    cancelled++
                    throw e
                }
            },
        )
    }

    @Test
    fun runs_work_on_first_trigger_after_grace_period() {
        val h = Harness()
        h.clock.t = 5_000
        h.gate.trigger()
        assertEquals(1, h.started)
        assertEquals(1, h.finished)
    }

    @Test
    fun skips_trigger_during_initial_grace_period() {
        // ViewModel baru sudah memuat datanya sendiri; tick pertama dari layar tidak boleh menembak server lagi.
        val h = Harness()
        h.clock.t = 1_000
        h.gate.trigger()
        assertEquals(0, h.started)
    }

    @Test
    fun skips_trigger_while_previous_work_is_still_running() {
        val gateOpen = CompletableDeferred<Unit>()
        val h = Harness(block = { gateOpen.await() })
        h.clock.t = 5_000
        h.gate.trigger()
        h.clock.t = 20_000
        h.gate.trigger()
        assertEquals(1, h.started)
        gateOpen.complete(Unit)
        assertEquals(1, h.finished)
    }

    @Test
    fun skips_trigger_within_minimum_gap_of_previous_start() {
        val h = Harness()
        h.clock.t = 5_000
        h.gate.trigger()
        h.clock.t = 9_999
        h.gate.trigger()
        assertEquals(1, h.started)
    }

    @Test
    fun runs_again_once_minimum_gap_has_passed() {
        val h = Harness()
        h.clock.t = 5_000
        h.gate.trigger()
        h.clock.t = 10_000
        h.gate.trigger()
        assertEquals(2, h.started)
    }

    @Test
    fun does_not_run_while_not_allowed_and_does_not_consume_the_gap() {
        val h = Harness()
        h.clock.t = 10_000
        h.allowed = false
        h.gate.trigger()
        assertEquals(0, h.started)
        // Begitu diizinkan lagi, langsung jalan: trigger yang ditolak tidak boleh menghabiskan jarak minimum.
        h.allowed = true
        h.clock.t = 10_001
        h.gate.trigger()
        assertEquals(1, h.started)
    }

    @Test
    fun cancel_stops_in_flight_work() {
        val never = CompletableDeferred<Unit>()
        val h = Harness(block = { never.await() })
        h.clock.t = 5_000
        h.gate.trigger()
        h.gate.cancel()
        assertEquals(1, h.cancelled)
        assertEquals(0, h.finished)
        assertTrue(h.leaked.isEmpty(), "pembatalan tidak boleh bocor sebagai error: ${h.leaked}")
    }

    @Test
    fun after_cancel_the_next_trigger_still_respects_the_minimum_gap() {
        val never = CompletableDeferred<Unit>()
        val h = Harness(block = { never.await() })
        h.clock.t = 10_000
        h.gate.trigger()
        h.gate.cancel()
        h.clock.t = 10_001
        h.gate.trigger()
        assertEquals(1, h.started)
        h.clock.t = 15_000
        h.gate.trigger()
        assertEquals(2, h.started)
    }

    @Test
    fun exception_in_work_does_not_leak_and_gate_stays_usable() {
        // Di viewModelScope tanpa handler, exception yang bocor = app crash gara-gara polling latar belakang.
        var boom = true
        val h = Harness(block = { if (boom) throw IllegalStateException("boom") })
        h.clock.t = 5_000
        h.gate.trigger()
        assertTrue(h.leaked.isEmpty(), "exception tidak boleh bocor: ${h.leaked}")
        boom = false
        h.clock.t = 10_000
        h.gate.trigger()
        assertEquals(2, h.started)
        assertEquals(1, h.finished)
    }

    @Test
    fun clock_moving_backwards_does_not_block_polling_forever() {
        val h = Harness()
        h.clock.t = 100_000
        h.gate.trigger()
        h.clock.t = 50_000 // jam perangkat diubah mundur
        h.gate.trigger()
        assertEquals(2, h.started)
    }
}
