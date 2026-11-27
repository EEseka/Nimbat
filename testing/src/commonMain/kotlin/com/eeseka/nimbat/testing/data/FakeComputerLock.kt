package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.guard.ComputerLock

class FakeComputerLock : ComputerLock {
    override var canLock: Boolean = true
    var lockCount = 0
        private set

    override fun lock() {
        lockCount++
    }
}
