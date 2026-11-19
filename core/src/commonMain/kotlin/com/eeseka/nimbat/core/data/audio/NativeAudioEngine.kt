package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

expect class NativeAudioEngine : AudioEngine {
    override fun microphone(): Flow<FloatArray>
    override suspend fun play(samples: FloatArray): EmptyResult<AudioError>
    override suspend fun loop(samples: FloatArray): EmptyResult<AudioError>
}
