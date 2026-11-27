package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.EmptyResult
import com.eeseka.nimbat.core.domain.util.Result
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class FakeAudioEngine : AudioEngine {
    private companion object {
        const val SILENT_BLOCK_SIZE = 4_096
        val SILENT_BLOCK_GAP = 85.milliseconds
    }

    var heard: List<FloatArray> = emptyList() // What the first recording hears before falling back to [background]
    var background = FloatArray(SILENT_BLOCK_SIZE) // What every recording hears on repeat after that: silence unless set
    var stallingRecordings = 0 // How many of the next recordings go quiet for good, like a mic another app took
    var isSpeakerHeard = true
    var microphoneError: AudioError? = null
    var playError: AudioError? = null
    var loopError: AudioError? = null
    var isLooping = false
        private set
    var lastPlayed: FloatArray? = null
        private set
    var lastLooped: FloatArray? = null
        private set

    private val speaker = MutableSharedFlow<FloatArray>(extraBufferCapacity = 16)

    override fun microphone(): Flow<FloatArray> = channelFlow {
        microphoneError?.let { throw AudioErrorException(it) }
        val firstBlocks = heard
        heard = emptyList()
        firstBlocks.forEach { send(it) }
        if (stallingRecordings > 0) {
            stallingRecordings--
            awaitCancellation()
        }
        launch { speaker.collect { send(it) } }
        while (true) {
            delay(SILENT_BLOCK_GAP)
            send(background.copyOf())
        }
    }

    override suspend fun play(samples: FloatArray): EmptyResult<AudioError> {
        playError?.let { return Result.Failure(it) }
        lastPlayed = samples
        if (isSpeakerHeard) speaker.emit(samples)
        return Result.Success(Unit)
    }

    override suspend fun loop(samples: FloatArray): EmptyResult<AudioError> {
        loopError?.let { return Result.Failure(it) }
        lastLooped = samples
        isLooping = true
        try {
            awaitCancellation()
        } finally {
            isLooping = false
        }
    }
}
