package com.example.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionReadContractTest {

    @Test
    fun read_valid_number_is_accepted_as_string() {
        val result = VisionReadContract.fromRemoteValues(
            status = "ok",
            numberText = "125.40",
            rawText = "125.40",
            reason = ""
        )

        assertEquals(VisionReadContract.Status.READ, result.status)
        assertEquals("125.40", result.number)
        assertEquals(125.40, result.number?.toDouble())
    }

    @Test
    fun not_readable_does_not_produce_a_number() {
        val result = VisionReadContract.fromRemoteValues(
            status = "not_readable",
            numberText = null,
            rawText = null,
            reason = "Pantalla ilegible"
        )

        assertEquals(VisionReadContract.Status.NOT_LEGIBLE, result.status)
        assertNull(result.number)
    }

    @Test
    fun conflict_does_not_produce_a_number() {
        val result = VisionReadContract.fromRemoteValues(
            status = "conflict",
            numberText = null,
            rawText = "12?4",
            reason = "Dígitos incompatibles"
        )

        assertEquals(VisionReadContract.Status.CONFLICT, result.status)
        assertNull(result.number)
    }

    @Test
    fun invalid_number_for_read_status_is_rejected() {
        val result = VisionReadContract.fromRemoteValues(
            status = "read",
            numberText = "125..40",
            rawText = "125..40",
            reason = ""
        )

        assertEquals(VisionReadContract.Status.CONFLICT, result.status)
        assertNull(result.number)
    }

    @Test
    fun unknown_status_is_rejected_without_inference() {
        val result = VisionReadContract.fromRemoteValues(
            status = "maybe",
            numberText = "125.40",
            rawText = "125.40",
            reason = ""
        )

        assertEquals(VisionReadContract.Status.CONFLICT, result.status)
        assertNull(result.number)
        assertTrue(result.reason!!.contains("desconocida"))
    }
}
