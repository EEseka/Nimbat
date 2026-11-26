package com.eeseka.nimbat.core.domain.wave

import kotlinx.coroutines.flow.Flow

interface WaveStorage {
    val bestBatDashScore: Flow<Int>

    suspend fun setBestBatDashScore(score: Int)
    suspend fun clear()
}
