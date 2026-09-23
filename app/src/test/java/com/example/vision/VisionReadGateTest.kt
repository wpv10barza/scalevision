package com.example.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VisionReadGateTest {

    @Test
    fun only_one_reader_can_be_active() {
        val gate = VisionReadGate()

        assertTrue(gate.tryAcquire())
        assertFalse(gate.tryAcquire())
        assertTrue(gate.isActive())

        gate.release()

        assertTrue(gate.tryAcquire())
        gate.release()
        assertFalse(gate.isActive())
    }

    @Test
    fun concurrent_acquisition_has_single_winner() {
        val gate = VisionReadGate()
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val done = CountDownLatch(8)
        val winners = java.util.concurrent.atomic.AtomicInteger(0)

        repeat(8) {
            pool.execute {
                try {
                    start.await()
                    if (gate.tryAcquire()) {
                        winners.incrementAndGet()
                    }
                } finally {
                    done.countDown()
                }
            }
        }

        start.countDown()
        assertTrue(done.await(2, TimeUnit.SECONDS))
        pool.shutdownNow()

        assertEquals(1, winners.get())
        gate.release()
    }

    @Test
    fun simulator_and_in_progress_states_block_new_read() {
        assertFalse(VisionReadPolicy.canStart(simulatorVisible = true, readInProgress = false))
        assertFalse(VisionReadPolicy.canStart(simulatorVisible = false, readInProgress = true))
        assertTrue(VisionReadPolicy.canStart(simulatorVisible = false, readInProgress = false))
    }
}
