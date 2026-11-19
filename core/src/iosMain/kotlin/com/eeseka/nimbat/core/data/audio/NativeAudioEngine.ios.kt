package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.EmptyResult
import com.eeseka.nimbat.core.domain.util.Result
import com.eeseka.nimbat.sound.SAMPLE_RATE
import com.eeseka.nimbat.sound.loopFadeIn
import com.eeseka.nimbat.sound.loopFadeOut
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineConfigurationChangeNotification
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferInterruptsAtLoop
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioPlayerNodeCompletionDataPlayedBack
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.AVFAudio.setPreferredSampleRate
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
actual class NativeAudioEngine : AudioEngine {
    private companion object {
        const val BLOCK_SIZE = 4_096 // ≈ 85 ms
    }

    // All engine work runs here, one task at a time: activating the session can stall the main thread
    private val audioThread = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + audioThread)

    private val engine = AVAudioEngine()
    private val player = AVAudioPlayerNode()
    private val loopPlayer = AVAudioPlayerNode() // separate, so a one-off sound can play over a loop
    private val monoFormat = AVAudioFormat(standardFormatWithSampleRate = SAMPLE_RATE.toDouble(), channels = 1u)
    private var isSessionReady = false
    private var looping: Pair<AVAudioPCMBuffer, AVAudioPCMBuffer>? = null // fade in, then loop

    actual override fun microphone(): Flow<FloatArray> = callbackFlow {
        if (!start()) throw AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE)
        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        if (format.sampleRate != SAMPLE_RATE.toDouble()) throw AudioErrorException(AudioError.SAMPLE_RATE_UNSUPPORTED)

        input.installTapOnBus(0u, bufferSize = BLOCK_SIZE.toUInt(), format = format) { buffer, _ ->
            buffer?.let { trySend(it.toSamples()) }
        }
        awaitClose { input.removeTapOnBus(0u) }
    }.flowOn(audioThread)

    actual override suspend fun play(samples: FloatArray): EmptyResult<AudioError> = withContext(audioThread) {
        if (!start()) return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
        val buffer = samples.toBuffer() ?: return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
        try {
            // An interruption can stop the "finished" callback from coming
            val longestWait = (samples.size * 1_000L / SAMPLE_RATE + 1_000).milliseconds
            withTimeoutOrNull(longestWait) {
                suspendCancellableCoroutine { continuation ->
                    // "Played back" fires once the sound has left the speaker, not when the player has taken it
                    player.scheduleBuffer(
                        buffer,
                        completionCallbackType = AVAudioPlayerNodeCompletionDataPlayedBack
                    ) { _ -> continuation.resume(Unit) }
                    player.play()
                }
            }
        } finally {
            player.stop()
        }
        Result.Success(Unit)
    }

    actual override suspend fun loop(samples: FloatArray): EmptyResult<AudioError> = withContext(audioThread) {
        if (!start()) return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
        val fadeIn = loopFadeIn(samples).toBuffer() ?: return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
        val loop = samples.toBuffer() ?: return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
        val fadeOut = loopFadeOut(samples).toBuffer() ?: return@withContext Result.Failure(AudioError.SPEAKER_UNAVAILABLE)
        try {
            looping = fadeIn to loop
            playLooping(fadeIn, loop)
            awaitCancellation()
        } finally {
            looping = null
            withContext(NonCancellable) {
                withTimeoutOrNull(1.seconds) {
                    suspendCancellableCoroutine { continuation ->
                        // Starts exactly where a pass of the loop ends, so the fade picks up without a jump
                        loopPlayer.scheduleBuffer(
                            fadeOut,
                            atTime = null,
                            options = AVAudioPlayerNodeBufferInterruptsAtLoop,
                            completionCallbackType = AVAudioPlayerNodeCompletionDataPlayedBack
                        ) { _ -> continuation.resume(Unit) }
                    }
                }
            }
            loopPlayer.stop()
        }
    }

    /** No Bluetooth on purpose: headset mics record at 16 kHz and can't hear the high tones. */
    private fun start(): Boolean {
        if (!isSessionReady) {
            val session = AVAudioSession.sharedInstance()
            val isConfigured = attempt { error ->
                session.setCategory(
                    AVAudioSessionCategoryPlayAndRecord,
                    mode = AVAudioSessionModeMeasurement,
                    options = AVAudioSessionCategoryOptionDefaultToSpeaker,
                    error = error
                )
            }
            if (!isConfigured) return false
            // Only a preference; the mic's rate check catches a refusal
            attempt { error -> session.setPreferredSampleRate(SAMPLE_RATE.toDouble(), error) }

            listOf(player, loopPlayer).forEach { node ->
                engine.attachNode(node)
                engine.connect(node, engine.mainMixerNode, monoFormat) // the mixer converts to the speaker's rate
            }
            engine.inputNode
            observeInterruptions()
            isSessionReady = true
        }
        if (engine.running) return true
        // An interruption deactivates the session
        if (!attempt { error -> AVAudioSession.sharedInstance().setActive(true, error) }) return false
        engine.prepare()
        return attempt { error -> engine.startAndReturnError(error) }
    }

    private fun playLooping(fadeIn: AVAudioPCMBuffer, loop: AVAudioPCMBuffer) {
        loopPlayer.scheduleBuffer(fadeIn, completionHandler = null)
        loopPlayer.scheduleBuffer(loop, atTime = null, options = AVAudioPlayerNodeBufferLoops, completionHandler = null)
        loopPlayer.play()
    }

    /** Restarts after an interruption or a headphone change, so a running mic and loop carry on. */
    private fun observeInterruptions() {
        val center = NSNotificationCenter.defaultCenter
        center.addObserverForName(AVAudioSessionInterruptionNotification, `object` = null, queue = null) { notification ->
            val type = (notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedLongValue
            if (type == AVAudioSessionInterruptionTypeEnded) scope.launch { resumeAfterStop() }
        }
        center.addObserverForName(AVAudioEngineConfigurationChangeNotification, `object` = engine, queue = null) { _ ->
            scope.launch { resumeAfterStop() }
        }
    }

    private fun resumeAfterStop() {
        if (!start()) return
        looping?.let { (fadeIn, loop) ->
            loopPlayer.stop()
            playLooping(fadeIn, loop)
        }
    }

    private fun AVAudioPCMBuffer.toSamples(): FloatArray {
        val channel = floatChannelData?.get(0) ?: return FloatArray(0)
        return FloatArray(frameLength.toInt()) { channel[it] }
    }

    private fun FloatArray.toBuffer(): AVAudioPCMBuffer? {
        val buffer = AVAudioPCMBuffer(pCMFormat = monoFormat, frameCapacity = size.toUInt())
        buffer.frameLength = size.toUInt()
        val channel = buffer.floatChannelData?.get(0) ?: return null
        forEachIndexed { i, value -> channel[i] = value }
        return buffer
    }

    /** Runs an iOS call that reports errors through a pointer. */
    private fun attempt(call: (CPointer<ObjCObjectVar<NSError?>>) -> Boolean): Boolean = memScoped {
        call(alloc<ObjCObjectVar<NSError?>>().ptr)
    }
}
