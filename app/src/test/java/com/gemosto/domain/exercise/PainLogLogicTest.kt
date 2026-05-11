package com.gemosto.domain.exercise

import com.gemosto.domain.model.PainEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class PainLogLogicTest {

    @Test
    fun `empty list returns 0`() {
        val result = PainLogLogic.computeConsecutiveHighPainCount(emptyList())
        assertEquals(0, result)
    }

    @Test
    fun `list with no high pain returns 0`() {
        val entries = listOf(
            createEntry(score = 5),
            createEntry(score = 6),
            createEntry(score = 4)
        )
        val result = PainLogLogic.computeConsecutiveHighPainCount(entries)
        assertEquals(0, result)
    }

    @Test
    fun `1 high pain at latest returns 1`() {
        val entries = listOf(
            createEntry(score = 8),
            createEntry(score = 5)
        )
        val result = PainLogLogic.computeConsecutiveHighPainCount(entries)
        assertEquals(1, result)
    }

    @Test
    fun `2 consecutive high pain at latest returns 2`() {
        val entries = listOf(
            createEntry(score = 9),
            createEntry(score = 8),
            createEntry(score = 5)
        )
        val result = PainLogLogic.computeConsecutiveHighPainCount(entries)
        assertEquals(2, result)
    }

    @Test
    fun `3 consecutive high pain at latest returns 3`() {
        val entries = listOf(
            createEntry(score = 8),
            createEntry(score = 8),
            createEntry(score = 8)
        )
        val result = PainLogLogic.computeConsecutiveHighPainCount(entries)
        assertEquals(3, result)
    }

    @Test
    fun `alternating high and low pain only counts latest consecutive block`() {
        val entries = listOf(
            createEntry(score = 8), // Latest = high
            createEntry(score = 5), // Low breaks the chain
            createEntry(score = 9)  // High, but old
        )
        val result = PainLogLogic.computeConsecutiveHighPainCount(entries)
        assertEquals(1, result)
    }

    @Test
    fun `low pain at latest returns 0 even if there are older high pains`() {
        val entries = listOf(
            createEntry(score = 5), // Latest = low
            createEntry(score = 8), // High, but old
            createEntry(score = 9)  // High, but old
        )
        val result = PainLogLogic.computeConsecutiveHighPainCount(entries)
        assertEquals(0, result)
    }

    private fun createEntry(score: Int): PainEntry {
        return PainEntry(
            id = UUID.randomUUID().toString(),
            userId = "user1",
            timestampMs = System.currentTimeMillis(),
            score = score,
            stoppedDueToPain = false,
            programId = "prog1",
            sessionStartedAtMs = null,
            sessionDurationMs = null
        )
    }
}
