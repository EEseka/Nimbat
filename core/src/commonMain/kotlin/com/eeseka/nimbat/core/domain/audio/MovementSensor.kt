package com.eeseka.nimbat.core.domain.audio

import com.eeseka.nimbat.sound.WatchRange
import com.eeseka.nimbat.sound.WatchReading
import kotlinx.coroutines.flow.Flow

interface MovementSensor {
    /** Plays the Doppler tone and watches for movement while collected; fails with [AudioErrorException]. */
    fun readings(range: WatchRange): Flow<WatchReading>
}
