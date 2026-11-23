package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.audio.SoundCheckProgress
import com.eeseka.nimbat.core.domain.audio.SoundChecker
import com.eeseka.nimbat.core.domain.audio.util.inBlocksOf
import com.eeseka.nimbat.core.domain.audio.util.recordWhileLooping
import com.eeseka.nimbat.core.domain.audio.util.recordWhilePlaying
import com.eeseka.nimbat.core.domain.util.onFailure
import com.eeseka.nimbat.core.domain.util.onSuccess
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.Finder
import com.eeseka.nimbat.sound.Role
import com.eeseka.nimbat.sound.UP_CHIRP
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.sound.pairSamples
import com.eeseka.nimbat.sound.toneClarityDb
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class AudioSoundChecker(
    private val audioEngine: AudioEngine,
    private val soundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : SoundChecker {

    private companion object {
        /** The pass mark: half of the 40 dB a clean hearing scores, so quiet speakers still pass and hiss (under 10) can't. */
        const val TONE_CLEAR_DB = 20.0
        /** Needing several clear readings in a row stops one lucky reading from passing; three take about a quarter of a second. */
        const val TONE_CLEAR_READINGS_IN_A_ROW = 3
        /** The clarity ring fills at this score. */
        const val TONE_FULL_CLARITY_DB = 40.0
        val TONE_CHECK_TIMEOUT = 5.seconds
        /** The chirp search always finds its best match, so this is the pass mark: just above noise (12–14 dB), far below a real chirp (50+). */
        const val CHIRP_CLEAR_DB = 20.0
    }

    override fun check(includeChirps: Boolean): Flow<SoundCheckProgress> = channelFlow {
        var clearInARow = 0

        val isToneHeard = try {
            withTimeoutOrNull(TONE_CHECK_TIMEOUT) {
                audioEngine.recordWhileLooping(dopplerTone())
                    .inBlocksOf(DOPPLER_BLOCK_SIZE)
                    .map { toneClarityDb(it) }
                    .flowOn(soundDispatcher)
                    .first { clarityDb ->
                        send(SoundCheckProgress.Listening((clarityDb / TONE_FULL_CLARITY_DB).toFloat().coerceIn(0f, 1f)))
                        clearInARow = if (clarityDb >= TONE_CLEAR_DB) clearInARow + 1 else 0
                        clearInARow >= TONE_CLEAR_READINGS_IN_A_ROW
                    }
                true
            } ?: false
        } catch (e: AudioErrorException) {
            send(SoundCheckProgress.Failed(e.error))
            return@channelFlow
        }

        if (!isToneHeard || !includeChirps) {
            send(SoundCheckProgress.Finished(isToneHeard))
            return@channelFlow
        }

        audioEngine.recordWhilePlaying(pairSamples(Role.Starter))
            .onSuccess { recording ->
                val hit = withContext(soundDispatcher) { Finder(recording, UP_CHIRP).find() }
                send(SoundCheckProgress.Finished(hit != null && hit.clarityDb >= CHIRP_CLEAR_DB))
            }
            .onFailure { error ->
                send(SoundCheckProgress.Failed(error))
            }
    }
}
