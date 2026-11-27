package com.eeseka.nimbat.core.data.guard

import com.eeseka.nimbat.core.domain.guard.ComputerLock

expect class NativeComputerLock() : ComputerLock {
    override val canLock: Boolean
    override fun lock()
}
