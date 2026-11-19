package com.eeseka.nimbat.core.domain.audio

import com.eeseka.nimbat.core.domain.util.EmptyResult
import com.eeseka.nimbat.sound.SAMPLE_RATE
import kotlinx.coroutines.flow.Flow

interface AudioEngine {

    /** Records at [SAMPLE_RATE] samples a second while collected; fails with [AudioErrorException]. */
    fun microphone(): Flow<FloatArray>

    /** Plays [samples] once and returns when they've finished playing, or stops early if cancelled. */
    suspend fun play(samples: FloatArray): EmptyResult<AudioError>

    /** Plays [samples] over and over until cancelled (e.g. the Doppler tone); returns only if it fails. */
    suspend fun loop(samples: FloatArray): EmptyResult<AudioError>
}
