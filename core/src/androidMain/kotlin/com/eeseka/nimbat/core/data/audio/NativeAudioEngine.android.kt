package com.eeseka.nimbat.core.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.getSystemService
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.EmptyResult
import com.eeseka.nimbat.core.domain.util.Result
import com.eeseka.nimbat.sound.SAMPLE_RATE
import com.eeseka.nimbat.sound.loopFadeIn
import com.eeseka.nimbat.sound.loopFadeOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

actual class NativeAudioEngine(context: Context) : AudioEngine {
    private companion object {
        const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT
        const val BLOCK_SIZE = 4_096 // ≈ 85 ms
    }

    private val audioManager = context.getSystemService<AudioManager>()!!

    @SuppressLint("MissingPermission") // asked for before any check starts
    actual override fun microphone(): Flow<FloatArray> = flow {
        val minBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            ENCODING
        )
        if (minBytes <= 0) throw AudioErrorException(AudioError.SAMPLE_RATE_UNSUPPORTED)

        val record = AudioRecord(
            microphoneSource(),
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            ENCODING,
            maxOf(minBytes, BLOCK_SIZE * Float.SIZE_BYTES * 2)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE)
        }

        try {
            record.startRecording()
            val block = FloatArray(BLOCK_SIZE)
            while (currentCoroutineContext().isActive) {
                val read = record.read(block, 0, block.size, AudioRecord.READ_BLOCKING)
                if (read < 0) throw AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE)
                if (read > 0) emit(block.copyOf(read))
            }
        } finally {
            record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    actual override suspend fun play(samples: FloatArray): EmptyResult<AudioError> =
        withContext(Dispatchers.IO) {
            val track = buildTrack(
                mode = AudioTrack.MODE_STREAM,
                bufferBytes = maxOf(minPlaybackBytes(), BLOCK_SIZE * Float.SIZE_BYTES)
            ) ?: return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)

            try {
                track.play()
                var written = 0
                while (written < samples.size && isActive) {
                    val count = minOf(BLOCK_SIZE, samples.size - written)
                    val result = track.write(samples, written, count, AudioTrack.WRITE_BLOCKING)
                    if (result < 0) return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
                    written += result
                }
                // A stream track only starts once its buffer is full, so a short sound is topped up with silence
                val padding = track.bufferSizeInFrames - samples.size
                if (padding > 0) track.write(FloatArray(padding), 0, padding, AudioTrack.WRITE_BLOCKING)
                awaitAtSpeaker(track, frames = samples.size.toLong())
                Result.Success(Unit)
            } finally {
                track.stop()
                track.release()
            }
        }

    actual override suspend fun loop(samples: FloatArray): EmptyResult<AudioError> =
        withContext(Dispatchers.IO) {
            val track = buildTrack(
                mode = AudioTrack.MODE_STREAM,
                bufferBytes = maxOf(minPlaybackBytes(), samples.size * Float.SIZE_BYTES)
            ) ?: return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)

            var framesWritten = 0L
            try {
                track.play()
                var block = loopFadeIn(samples)
                while (isActive) {
                    if (track.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING) < 0) {
                        return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
                    }
                    framesWritten += block.size
                    block = samples
                }
                Result.Success(Unit)
            } finally {
                withContext(NonCancellable) {
                    val tail = loopFadeOut(samples)
                    if (track.write(tail, 0, tail.size, AudioTrack.WRITE_BLOCKING) > 0) {
                        awaitAtSpeaker(track, frames = framesWritten + tail.size)
                    }
                }
                track.stop()
                track.release()
            }
        }

    /** Waits until [frames] have left the speaker; the playhead runs ahead, and stopping on it cut sounds off. */
    private suspend fun awaitAtSpeaker(track: AudioTrack, frames: Long) {
        val atSpeaker = AudioTimestamp()
        val playedFrames = track.playbackHeadPosition.toLong()
        val longestWait = ((frames - playedFrames).coerceAtLeast(0) * 1_000 / SAMPLE_RATE + 1_000).milliseconds
        withTimeoutOrNull(longestWait) {
            while (!(track.getTimestamp(atSpeaker) && atSpeaker.framePosition >= frames)) delay(10.milliseconds)
        }
    }

    /** UNPROCESSED keeps the high tones; phones without it fall back to the next least processed source. */
    private fun microphoneSource(): Int {
        val unprocessed = audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
        return if (unprocessed == "true") MediaRecorder.AudioSource.UNPROCESSED
        else MediaRecorder.AudioSource.VOICE_RECOGNITION
    }

    private fun buildTrack(mode: Int, bufferBytes: Int): AudioTrack? = try {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(ENCODING)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(mode)
            .setBufferSizeInBytes(bufferBytes)
            .build()
    } catch (_: UnsupportedOperationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun minPlaybackBytes() =
        AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING)
}
