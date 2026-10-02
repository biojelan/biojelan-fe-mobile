package id.biojelan.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangeDetectionTest {
    private data class Tx(val id: String, val status: String)

    private fun hasNew(before: List<Tx>, after: List<Tx>) =
        hasNewActionable(before, after, key = { "${it.id}:${it.status}" }, actionable = { it.status == "pending" || it.status == "cancel_requested" })

    // ---------------------------------------------------------------- hasNewActionable

    @Test
    fun new_pending_item_is_detected() {
        assertTrue(hasNew(emptyList(), listOf(Tx("a", "pending"))))
    }

    @Test
    fun already_known_pending_item_is_not_new() {
        val same = listOf(Tx("a", "pending"))
        assertFalse(hasNew(same, same))
    }

    @Test
    fun pending_item_that_disappeared_is_not_new() {
        assertFalse(hasNew(listOf(Tx("a", "pending")), emptyList()))
    }

    @Test
    fun pending_turning_into_cancel_request_counts_as_new() {
        assertTrue(hasNew(listOf(Tx("a", "accepted")), listOf(Tx("a", "cancel_requested"))))
    }

    @Test
    fun new_item_that_needs_no_action_is_ignored() {
        assertFalse(hasNew(emptyList(), listOf(Tx("a", "accepted"), Tx("b", "rejected"))))
    }

    @Test
    fun pending_turning_into_accepted_is_not_new() {
        assertFalse(hasNew(listOf(Tx("a", "pending")), listOf(Tx("a", "accepted"))))
    }

    // ---------------------------------------------------------------- changedStatus

    private fun changed(before: List<Tx>, after: List<Tx>) = changedStatus(before, after, id = { it.id }, status = { it.status })

    @Test
    fun item_whose_status_changed_is_returned() {
        val result = changed(listOf(Tx("a", "pending")), listOf(Tx("a", "accepted")))
        assertEquals(listOf(Tx("a", "accepted")), result)
    }

    @Test
    fun unchanged_item_is_not_returned() {
        assertEquals(emptyList(), changed(listOf(Tx("a", "pending")), listOf(Tx("a", "pending"))))
    }

    @Test
    fun brand_new_item_is_not_a_status_change() {
        assertEquals(emptyList(), changed(listOf(Tx("a", "pending")), listOf(Tx("a", "pending"), Tx("b", "pending"))))
    }

    @Test
    fun removed_item_is_not_a_status_change() {
        assertEquals(emptyList(), changed(listOf(Tx("a", "pending")), emptyList()))
    }

    // ---------------------------------------------------------------- shouldAnnounceNewAssignment

    @Test
    fun announces_when_a_different_assignment_appears_after_a_good_load() {
        assertTrue(shouldAnnounceNewAssignment(previousLoadedOk = true, previousId = null, incomingId = "p1"))
        assertTrue(shouldAnnounceNewAssignment(previousLoadedOk = true, previousId = "p1", incomingId = "p2"))
    }

    @Test
    fun does_not_announce_the_same_assignment_again() {
        assertFalse(shouldAnnounceNewAssignment(previousLoadedOk = true, previousId = "p1", incomingId = "p1"))
    }

    @Test
    fun does_not_announce_when_there_is_no_assignment() {
        assertFalse(shouldAnnounceNewAssignment(previousLoadedOk = true, previousId = "p1", incomingId = null))
    }

    @Test
    fun does_not_announce_when_the_previous_load_had_failed() {
        // Kalau muat sebelumnya gagal, "tidak ada pickup" di state itu bukan fakta — jangan klaim ini baru.
        assertFalse(shouldAnnounceNewAssignment(previousLoadedOk = false, previousId = null, incomingId = "p1"))
    }

    @Test
    fun blank_incoming_id_is_not_an_assignment() {
        assertFalse(shouldAnnounceNewAssignment(previousLoadedOk = true, previousId = null, incomingId = ""))
    }
}
