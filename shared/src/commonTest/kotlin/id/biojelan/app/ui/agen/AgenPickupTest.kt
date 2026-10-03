package id.biojelan.app.ui.agen

import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.PickupStatusDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgenPickupTest {
    private val arrivedAt = "2026-08-10T10:00:00Z"

    private fun pickup(status: String, updatedAt: String = arrivedAt) =
        PickupStatusDto(pickupId = "pkp-007", status = status, updatedAt = updatedAt)

    private fun tx(status: String, createdAt: String, liters: Double = 100.0, id: String = "t") =
        DriverTransactionDto(transactionId = id, agenId = "A", volumeLiter = liters, status = status, createdAt = createdAt)

    @Test
    fun earlyStatusesMapDirectly() {
        assertEquals(AgenPickupStep.Assigned, agenPickupStep(pickup("ASSIGNED"), emptyList()))
        assertEquals(AgenPickupStep.OnTheWay, agenPickupStep(pickup("OTW"), emptyList()))
        assertEquals(AgenPickupStep.Completed, agenPickupStep(pickup("COMPLETED"), emptyList()))
        assertEquals(AgenPickupStep.Cancelled, agenPickupStep(pickup("CANCELLED"), emptyList()))
        assertEquals(AgenPickupStep.Unknown, agenPickupStep(pickup("???"), emptyList()))
    }

    @Test
    fun arrivedWithoutTransactionStaysArrived() {
        assertEquals(AgenPickupStep.Arrived, agenPickupStep(pickup("ARRIVED"), emptyList()))
    }

    @Test
    fun arrivedWithPendingTransactionAwaitsConfirmation() {
        val txs = listOf(tx("PENDING", "2026-08-10T10:05:00Z"))
        assertEquals(AgenPickupStep.AwaitingConfirm, agenPickupStep(pickup("ARRIVED"), txs))
    }

    @Test
    fun arrivedWithAcceptedTransactionIsCompleted() {
        val txs = listOf(tx("ACCEPTED", "2026-08-10T10:05:00Z"))
        assertEquals(AgenPickupStep.Completed, agenPickupStep(pickup("ARRIVED"), txs))
    }

    @Test
    fun rejectedTransactionGoesBackToArrived() {
        val txs = listOf(tx("REJECTED", "2026-08-10T10:05:00Z"))
        assertEquals(AgenPickupStep.Arrived, agenPickupStep(pickup("ARRIVED"), txs))
    }

    @Test
    fun oldTransactionFromBeforeArrivalIsIgnored() {
        val txs = listOf(tx("ACCEPTED", "2026-08-09T10:00:00Z"))
        assertEquals(AgenPickupStep.Arrived, agenPickupStep(pickup("ARRIVED"), txs))
    }

    @Test
    fun latestTransactionWins() {
        val txs = listOf(
            tx("REJECTED", "2026-08-10T10:05:00Z", id = "1"),
            tx("PENDING", "2026-08-10T10:20:00Z", id = "2"),
        )
        assertEquals(AgenPickupStep.AwaitingConfirm, agenPickupStep(pickup("ARRIVED"), txs))
    }

    @Test
    fun stepperIndexes() {
        assertEquals(0, AgenPickupStep.Assigned.stepperIndex())
        assertEquals(1, AgenPickupStep.OnTheWay.stepperIndex())
        assertEquals(2, AgenPickupStep.Arrived.stepperIndex())
        assertEquals(3, AgenPickupStep.AwaitingConfirm.stepperIndex())
        assertEquals(5, AgenPickupStep.Completed.stepperIndex())
        assertEquals(-1, AgenPickupStep.Cancelled.stepperIndex())
    }

    @Test
    fun requestedVolumeOnlyForRelevantTransactions() {
        val p = pickup("ARRIVED")
        assertNull(requestedPickupVolume(pickup("ASSIGNED"), listOf(tx("PENDING", "2026-08-10T10:05:00Z"))))
        assertNull(requestedPickupVolume(p, emptyList()))
        assertNull(requestedPickupVolume(p, listOf(tx("REJECTED", "2026-08-10T10:05:00Z"))))
        assertEquals(120.0, requestedPickupVolume(p, listOf(tx("PENDING", "2026-08-10T10:05:00Z", 120.0))))
        assertEquals(80.0, requestedPickupVolume(p, listOf(tx("ACCEPTED", "2026-08-10T10:05:00Z", 80.0))))
    }
}
