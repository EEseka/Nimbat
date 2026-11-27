package com.eeseka.nimbat.core.data.guard

import com.eeseka.nimbat.core.domain.guard.ComputerLock

actual class NativeComputerLock : ComputerLock {
    actual override val canLock: Boolean = false

    actual override fun lock() = Unit
}
