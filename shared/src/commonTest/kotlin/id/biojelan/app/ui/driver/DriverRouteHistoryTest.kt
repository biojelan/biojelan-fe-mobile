package id.biojelan.app.ui.driver

import id.biojelan.app.core.localEpochDayOf
import id.biojelan.app.data.remote.DriverTransactionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Waktu uji dibuat sekitar tengah hari UTC supaya tanggal lokalnya sama di zona waktu mana pun
 * (WIB, WITA, WIT), jadi tes tidak bergantung pada zona perangkat.
 */
class DriverRouteHistoryTest {
    private fun tx(
        id: String,
        agen: String,
        liters: Double,
        status: String,
        createdAt: String,
        price: Long = 0L,
    ) = DriverTransactionDto(
        transactionId = id,
        agenId = agen,
        agenName = "Agen $agen",
        volumeLiter = liters,
        totalPrice = price,
        status = status,
        createdAt = createdAt,
    )

    @Test
    fun emptyInputGivesEmptyHistory() {
        assertTrue(buildRouteHistory(emptyList()).isEmpty())
    }

    @Test
    fun groupsByDayAndSumsCountedTransactions() {
        val h = buildRouteHistory(
            listOf(
                tx("1", "A", 100.0, "ACCEPTED", "2026-08-10T11:00:00Z", 1_000),
                tx("2", "B", 50.5, "PENDING", "2026-08-10T12:30:00Z", 500),
                tx("3", "A", 20.0, "ACCEPTED", "2026-08-09T12:00:00Z", 200),
            ),
        )
        assertEquals(2, h.size)
        val day10 = h[0]
        assertEquals(2, day10.agenCount)
        assertEquals(150.5, day10.liters)
        assertEquals(1_500L, day10.value)
        assertEquals(90, day10.spanMinutes)
        assertEquals(1, day10.awaitingCount)
        assertEquals(listOf("1", "2"), day10.transactions.map { it.transactionId })
    }

    @Test
    fun newestDayFirst() {
        val h = buildRouteHistory(
            listOf(
                tx("1", "A", 1.0, "ACCEPTED", "2026-08-07T12:00:00Z"),
                tx("2", "A", 1.0, "ACCEPTED", "2026-08-10T12:00:00Z"),
                tx("3", "A", 1.0, "ACCEPTED", "2026-08-09T12:00:00Z"),
            ),
        )
        assertEquals(listOf("2", "3", "1"), h.map { it.transactions.single().transactionId })
        assertTrue(h.zipWithNext().all { (a, b) -> a.epochDay > b.epochDay })
    }

    @Test
    fun rejectedAndCancelledAreExcluded() {
        val h = buildRouteHistory(
            listOf(
                tx("1", "A", 100.0, "REJECTED", "2026-08-10T12:00:00Z"),
                tx("2", "B", 100.0, "CANCELLED", "2026-08-10T12:10:00Z"),
                tx("3", "C", 30.0, "ACCEPTED", "2026-08-10T12:20:00Z"),
            ),
        )
        assertEquals(1, h.size)
        assertEquals(1, h[0].agenCount)
        assertEquals(30.0, h[0].liters)
        assertNull(h[0].spanMinutes)
    }

    @Test
    fun dayWithOnlyRejectedIsSkipped() {
        val h = buildRouteHistory(listOf(tx("1", "A", 10.0, "REJECTED", "2026-08-10T12:00:00Z")))
        assertTrue(h.isEmpty())
    }

    @Test
    fun sameAgenVisitedTwiceCountsOnce() {
        val h = buildRouteHistory(
            listOf(
                tx("1", "A", 10.0, "ACCEPTED", "2026-08-10T10:00:00Z"),
                tx("2", "A", 15.0, "ACCEPTED", "2026-08-10T12:00:00Z"),
            ),
        )
        assertEquals(1, h[0].agenCount)
        assertEquals(25.0, h[0].liters)
        assertEquals(120, h[0].spanMinutes)
    }

    @Test
    fun cancelRequestedCountsAsAwaiting() {
        val h = buildRouteHistory(listOf(tx("1", "A", 10.0, "CANCEL_REQUESTED", "2026-08-10T12:00:00Z")))
        assertEquals(1, h[0].awaitingCount)
    }

    @Test
    fun unparseableDateIsSkipped() {
        val h = buildRouteHistory(listOf(tx("1", "A", 10.0, "ACCEPTED", "bukan-tanggal")))
        assertTrue(h.isEmpty())
    }

    @Test
    fun epochDayMatchesLocalDay() {
        val h = buildRouteHistory(listOf(tx("1", "A", 10.0, "ACCEPTED", "2026-08-10T12:00:00Z")))
        assertEquals(localEpochDayOf("2026-08-10T12:00:00Z"), h[0].epochDay)
    }

    @Test
    fun formatSpanReadable() {
        assertEquals("2j 10m", formatSpan(130))
        assertEquals("1j 25m", formatSpan(85))
        assertEquals("45m", formatSpan(45))
        assertEquals("2j 00m", formatSpan(120))
        assertEquals("0m", formatSpan(-5))
    }
}
