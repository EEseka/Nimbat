package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.wave.WaveStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWaveStorage : WaveStorage {
    private val bestBatDashScoreFlow = MutableStateFlow(0)
    override val bestBatDashScore: Flow<Int> = bestBatDashScoreFlow

    override suspend fun setBestBatDashScore(score: Int) {
        bestBatDashScoreFlow.value = score
    }

    override suspend fun clear() {
        bestBatDashScoreFlow.value = 0
    }
}
