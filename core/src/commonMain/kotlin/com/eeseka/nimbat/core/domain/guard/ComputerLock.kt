package com.eeseka.nimbat.core.domain.guard

interface ComputerLock {
    val canLock: Boolean

    fun lock()
}
