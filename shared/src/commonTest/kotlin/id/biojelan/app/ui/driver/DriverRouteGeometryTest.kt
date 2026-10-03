package id.biojelan.app.ui.driver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DriverRouteGeometryTest {
    private fun stop(id: String, status: StopStatus, pickup: Boolean) =
        RouteStop(agenId = id, name = "Agen $id", volumeLiter = null, status = status, isPickup = pickup, pickupId = if (pickup) "p$id" else null)

    @Test
    fun projectKeepsNorthOnTopAndWestOnLeft() {
        val spots = projectToMap(listOf(-5.40 to 105.20, -5.30 to 105.30))
        val south = assertNotNull(spots[0])
        val north = assertNotNull(spots[1])
        assertTrue(north.y < south.y, "utara harus di atas")
        assertTrue(south.x < north.x, "barat harus di kiri")
    }

    @Test
    fun projectStaysInsideCard() {
        val spots = projectToMap(listOf(-5.40 to 105.20, -5.30 to 105.30, -5.35 to 105.25, -5.50 to 105.45)).filterNotNull()
        assertEquals(4, spots.size)
        spots.forEach { assertTrue(it.x in 0f..1f && it.y in 0f..1f) }
    }

    @Test
    fun projectSameLatitudeCentersVertically() {
        val spots = projectToMap(listOf(-5.40 to 105.20, -5.40 to 105.30)).filterNotNull()
        spots.forEach { assertEquals(0.5f, it.y, 0.001f) }
    }

    @Test
    fun projectReturnsNullForMissingCoordinates() {
        val spots = projectToMap(listOf(-5.40 to 105.20, 0.0 to 0.0, null, -5.30 to 105.30))
        assertNotNull(spots[0])
        assertNull(spots[1])
        assertNull(spots[2])
        assertNotNull(spots[3])
    }

    @Test
    fun projectFallsBackWhenLessThanTwoValidOrIdentical() {
        assertTrue(projectToMap(listOf(-5.40 to 105.20, 0.0 to 0.0)).all { it == null })
        assertTrue(projectToMap(listOf(-5.40 to 105.20, -5.40 to 105.20)).all { it == null })
        assertTrue(projectToMap(emptyList()).isEmpty())
    }

    @Test
    fun progressEnRouteHasPreviousCheckpointAndDestination() {
        val p = routeProgress(listOf(stop("1", StopStatus.Completed, false), stop("2", StopStatus.OnTheWay, true)))
        assertEquals(RoutePhase.EnRoute, p.phase)
        assertEquals("1", p.from?.agenId)
        assertEquals("2", p.to?.agenId)
        assertEquals(1, p.currentIndex)
    }

    @Test
    fun progressFirstStopHasNoCheckpoint() {
        val p = routeProgress(listOf(stop("1", StopStatus.Assigned, true)))
        assertEquals(RoutePhase.Ready, p.phase)
        assertNull(p.from)
        assertEquals("1", p.to?.agenId)
    }

    @Test
    fun progressMapsArrivedAndAwaiting() {
        assertEquals(RoutePhase.Arrived, routeProgress(listOf(stop("1", StopStatus.Arrived, true))).phase)
        assertEquals(RoutePhase.Awaiting, routeProgress(listOf(stop("1", StopStatus.AwaitingConfirm, true))).phase)
    }

    @Test
    fun progressFinishedWhenNoActivePickup() {
        val p = routeProgress(listOf(stop("1", StopStatus.Completed, false), stop("2", StopStatus.Completed, true)))
        assertEquals(RoutePhase.Finished, p.phase)
        assertEquals("2", p.from?.agenId)
        assertNull(p.to)
        assertEquals(-1, p.currentIndex)
    }

    @Test
    fun progressIdleWhenEmpty() {
        val p = routeProgress(emptyList())
        assertEquals(RoutePhase.Idle, p.phase)
        assertNull(p.from)
        assertNull(p.to)
    }
}
