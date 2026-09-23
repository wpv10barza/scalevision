package com.example.vision

import java.util.concurrent.atomic.AtomicBoolean

class VisionReadGate {

    private val active = AtomicBoolean(false)

    fun tryAcquire(): Boolean =
        active.compareAndSet(false, true)

    fun release() {
        active.set(false)
    }

    fun isActive(): Boolean = active.get()
}
