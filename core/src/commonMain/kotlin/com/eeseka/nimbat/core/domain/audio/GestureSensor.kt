package com.eeseka.nimbat.core.domain.audio

import com.eeseka.nimbat.sound.GestureReading
import kotlinx.coroutines.flow.Flow

interface GestureSensor {
    /** Plays the Doppler tone and reads hand gestures from its echoes while collected; fails with [AudioErrorException]. */
    fun readings(): Flow<GestureReading>
}
