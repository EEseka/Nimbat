package com.eeseka.nimbat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform