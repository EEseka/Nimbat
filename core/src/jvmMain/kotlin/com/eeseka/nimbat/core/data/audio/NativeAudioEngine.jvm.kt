package com.eeseka.nimbat.core.data.audio

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
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine
import kotlin.time.Duration.Companion.milliseconds

actual class NativeAudioEngine : AudioEngine {
    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val BLOCK_SIZE = 4_096 // ≈ 85 ms
        const val SILENCE_FRAMES = 4_800 // 100 ms
    }

    private val format = AudioFormat(
        SAMPLE_RATE.toFloat(),
        16,
        1,
        true,
        false
    )

    actual override fun microphone(): Flow<FloatArray> = flow {
        val line = when (val opened = openLine<TargetDataLine>(AudioError.MICROPHONE_UNAVAILABLE)) {
            is Result.Failure -> throw AudioErrorException(opened.error)
            is Result.Success -> opened.data
        }
        try {
            line.start()
            val bytes = ByteArray(BLOCK_SIZE * BYTES_PER_SAMPLE)
            while (currentCoroutineContext().isActive) {
                val read = line.read(bytes, 0, bytes.size)
                if (read > 0) emit(bytes.toSamples(read))
            }
        } finally {
            line.stop()
            line.close()
        }
    }.flowOn(Dispatchers.IO)

    actual override suspend fun play(samples: FloatArray): EmptyResult<AudioError> = withContext(Dispatchers.IO) {
        val line = when (val opened = openLine<SourceDataLine>(AudioError.SPEAKER_UNAVAILABLE)) {
            is Result.Failure -> return@withContext opened
            is Result.Success -> opened.data
        }
        try {
            line.start()
            var written = 0
            while (written < samples.size && isActive) {
                val count = minOf(BLOCK_SIZE, samples.size - written)
                val bytes = samples.toBytes(written, count)
                line.write(bytes, 0, bytes.size)
                written += count
            }
            finishWithSilence(line, framesWritten = written.toLong())
            Result.Success(Unit)
        } finally {
            line.stop()
            line.close()
        }
    }

    actual override suspend fun loop(samples: FloatArray): EmptyResult<AudioError> = withContext(Dispatchers.IO) {
        val line = when (val opened = openLine<SourceDataLine>(AudioError.SPEAKER_UNAVAILABLE)) {
            is Result.Failure -> return@withContext opened
            is Result.Success -> opened.data
        }
        val loopBytes = samples.toBytes(0, samples.size)
        var framesWritten = 0L
        try {
            line.start()
            var bytes = loopFadeIn(samples).let { it.toBytes(0, it.size) }
            while (isActive) {
                line.write(bytes, 0, bytes.size)
                framesWritten += samples.size
                bytes = loopBytes
            }
            Result.Success(Unit)
        } finally {
            withContext(NonCancellable) {
                val tail = loopFadeOut(samples)
                line.write(tail.toBytes(0, tail.size), 0, tail.size * BYTES_PER_SAMPLE)
                finishWithSilence(line, framesWritten = framesWritten + tail.size)
            }
            line.stop()
            line.close()
        }
    }

    /** Java reports frames played a little before the speaker does, so closing then could click; this ends on silence. */
    private suspend fun finishWithSilence(line: SourceDataLine, framesWritten: Long) {
        val silence = ByteArray(SILENCE_FRAMES * BYTES_PER_SAMPLE)
        line.write(silence, 0, silence.size)
        val frames = framesWritten + SILENCE_FRAMES
        val longestWait = ((frames - line.longFramePosition).coerceAtLeast(0) * 1_000 / SAMPLE_RATE + 1_000).milliseconds
        // Not drain(): it blocks forever if the device disappears, and can't be cancelled
        withTimeoutOrNull(longestWait) {
            while (line.longFramePosition < frames) delay(10.milliseconds)
        }
    }

    private inline fun <reified T : DataLine> openLine(unavailable: AudioError): Result<T, AudioError> {
        val info = DataLine.Info(T::class.java, format)
        if (!AudioSystem.isLineSupported(info)) return Result.Failure(AudioError.SAMPLE_RATE_UNSUPPORTED)
        return try {
            val line = AudioSystem.getLine(info) as T
            when (line) {
                is TargetDataLine -> line.open(format, BLOCK_SIZE * BYTES_PER_SAMPLE * 2)
                is SourceDataLine -> line.open(format, BLOCK_SIZE * BYTES_PER_SAMPLE * 2)
            }
            Result.Success(line)
        } catch (_: LineUnavailableException) {
            Result.Failure(unavailable)
        } catch (_: SecurityException) {
            Result.Failure(unavailable)
        }
    }

    /** 16-bit little-endian → −1 to 1. */
    private fun ByteArray.toSamples(byteCount: Int): FloatArray =
        FloatArray(byteCount / BYTES_PER_SAMPLE) { i ->
            val low = this[2 * i].toInt() and 0xFF
            val high = this[2 * i + 1].toInt()
            ((high shl 8) or low) / 32_768f
        }

    /** −1 to 1 → 16-bit little-endian. */
    private fun FloatArray.toBytes(from: Int, count: Int): ByteArray {
        val bytes = ByteArray(count * BYTES_PER_SAMPLE)
        for (i in 0 until count) {
            val value = (this[from + i].coerceIn(-1f, 1f) * 32_767).toInt()
            bytes[2 * i] = value.toByte()
            bytes[2 * i + 1] = (value shr 8).toByte()
        }
        return bytes
    }
}
