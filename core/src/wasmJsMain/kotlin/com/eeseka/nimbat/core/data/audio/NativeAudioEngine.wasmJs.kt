@file:OptIn(ExperimentalWasmJsInterop::class)

package com.eeseka.nimbat.core.data.audio

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.util.EmptyResult
import com.eeseka.nimbat.core.domain.util.Result
import com.eeseka.nimbat.sound.LOOP_FADE_SECONDS
import com.eeseka.nimbat.sound.SAMPLE_RATE
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.js.JsException
import kotlin.js.Promise
import kotlin.time.Duration.Companion.milliseconds

actual class NativeAudioEngine : AudioEngine {
    private companion object {
        const val BLOCK_SIZE = 4_096 // ≈ 85 ms
    }

    private var context: JsAny? = null
    private var isCaptureLoaded = false

    actual override fun microphone(): Flow<FloatArray> = callbackFlow {
        // Mic first: browsers start audio more readily once a mic is live
        val stream = try {
            openMicrophone().await()
        } catch (_: JsException) {
            throw AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE)
        }
        val audioContext = when (val started = startContext()) {
            is Result.Failure -> {
                stopStream(stream)
                throw AudioErrorException(started.error)
            }
            is Result.Success -> started.data
        }
        if (!isCaptureLoaded) {
            try {
                loadCapture(audioContext).await<JsAny?>()
                isCaptureLoaded = true
            } catch (_: JsException) {
                stopStream(stream)
                throw AudioErrorException(AudioError.MICROPHONE_UNAVAILABLE)
            }
        }
        val capture = try {
            startCapture(audioContext, stream, BLOCK_SIZE) { block ->
                trySend(FloatArray(jsLength(block)) { jsFloatAt(block, it) })
            }
        } catch (_: JsException) {
            // Firefox can't connect a mic running at a different rate from the context
            stopStream(stream)
            throw AudioErrorException(AudioError.SAMPLE_RATE_UNSUPPORTED)
        }
        awaitClose { stopCapture(capture) }
    }

    actual override suspend fun play(samples: FloatArray): EmptyResult<AudioError> {
        val audioContext = when (val started = startContext()) {
            is Result.Failure -> return started
            is Result.Success -> started.data
        }
        val source = startSource(audioContext, samples, loop = false)
        try {
            // A suspended context never fires "ended"
            val longestWait = (samples.size * 1_000L / SAMPLE_RATE + 1_000).milliseconds
            withTimeoutOrNull(longestWait) {
                suspendCancellableCoroutine { continuation ->
                    onSourceEnded(source) { continuation.resume(Unit) }
                }
            }
        } finally {
            stopSource(source, LOOP_FADE_SECONDS) // only audible if cancelled mid-sound
        }
        return Result.Success(Unit)
    }

    actual override suspend fun loop(samples: FloatArray): EmptyResult<AudioError> {
        val audioContext = when (val started = startContext()) {
            is Result.Failure -> return started
            is Result.Success -> started.data
        }
        val source = startSource(audioContext, samples, loop = true)
        try {
            awaitCancellation()
        } finally {
            stopSource(source, LOOP_FADE_SECONDS)
        }
    }

    /** Browsers keep the context paused until a tap, so it's resumed on every use. */
    private suspend fun startContext(): Result<JsAny, AudioError> {
        val audioContext = context ?: try {
            createAudioContext(SAMPLE_RATE).also { context = it }
        } catch (_: JsException) {
            return Result.Failure(AudioError.SAMPLE_RATE_UNSUPPORTED)
        }
        if (jsSampleRate(audioContext) != SAMPLE_RATE) return Result.Failure(AudioError.SAMPLE_RATE_UNSUPPORTED)
        resumeContext(audioContext).await()
        return Result.Success(audioContext)
    }

    /** A loop fades in; a one-off sound carries its own fades. */
    private fun startSource(audioContext: JsAny, samples: FloatArray, loop: Boolean): JsAny {
        val buffer = createBuffer(audioContext, samples.size, SAMPLE_RATE)
        val channel = jsChannelData(buffer)
        samples.forEachIndexed { i, value -> jsSetFloatAt(channel, i, value) }
        return startBufferSource(audioContext, buffer, loop, fadeInSeconds = if (loop) LOOP_FADE_SECONDS else 0.0)
    }
}

// Resumes after an interruption (a call, a hidden tab) once the page is visible
private fun createAudioContext(sampleRate: Int): JsAny = js(
    """{
        const context = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: sampleRate });
        const resumeIfVisible = () => {
            if (context.state !== 'running' && context.state !== 'closed' && document.visibilityState === 'visible') {
                context.resume().catch(() => {});
            }
        };
        context.onstatechange = resumeIfVisible;
        document.addEventListener('visibilitychange', resumeIfVisible);
        return context;
    }"""
)

private fun jsSampleRate(audioContext: JsAny): Int = js("audioContext.sampleRate")

private fun resumeContext(audioContext: JsAny): Promise<JsAny?> = js("audioContext.resume()")

// These filters treat the high tones as noise
private fun openMicrophone(): Promise<JsAny> = js(
    "navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: false, noiseSuppression: false, autoGainControl: false, channelCount: 1 } })"
)

// Runs on the audio thread, so a busy UI can't drop samples
private fun loadCapture(audioContext: JsAny): Promise<JsAny?> = js(
    """{
        const code = `
            class NimbatCapture extends AudioWorkletProcessor {
                constructor(options) {
                    super();
                    this.size = options.processorOptions.blockSize;
                    this.block = new Float32Array(this.size);
                    this.filled = 0;
                }
                process(inputs) {
                    const channel = inputs[0][0];
                    if (channel) {
                        for (let i = 0; i < channel.length; i++) {
                            this.block[this.filled++] = channel[i];
                            if (this.filled === this.size) {
                                this.port.postMessage(this.block, [this.block.buffer]);
                                this.block = new Float32Array(this.size);
                                this.filled = 0;
                            }
                        }
                    }
                    return true;
                }
            }
            registerProcessor('nimbat-capture', NimbatCapture);
        `;
        const url = URL.createObjectURL(new Blob([code], { type: 'application/javascript' }));
        return audioContext.audioWorklet.addModule(url).finally(() => URL.revokeObjectURL(url));
    }"""
)

// The worklet only runs while connected to the output; it writes silence
private fun startCapture(audioContext: JsAny, stream: JsAny, blockSize: Int, onBlock: (JsAny) -> Unit): JsAny = js(
    """{
        const source = audioContext.createMediaStreamSource(stream);
        const node = new AudioWorkletNode(audioContext, 'nimbat-capture', { processorOptions: { blockSize: blockSize } });
        node.port.onmessage = (event) => onBlock(event.data);
        source.connect(node);
        node.connect(audioContext.destination);
        return { source: source, node: node, stream: stream };
    }"""
)

private fun stopCapture(capture: JsAny): Unit = js(
    """{
        capture.node.port.onmessage = null;
        capture.node.disconnect();
        capture.source.disconnect();
        capture.stream.getTracks().forEach((track) => track.stop());
    }"""
)

private fun stopStream(stream: JsAny): Unit = js("{ stream.getTracks().forEach((track) => track.stop()); }")

private fun createBuffer(audioContext: JsAny, length: Int, sampleRate: Int): JsAny =
    js("audioContext.createBuffer(1, length, sampleRate)")

private fun jsChannelData(buffer: JsAny): JsAny = js("buffer.getChannelData(0)")

private fun jsLength(array: JsAny): Int = js("array.length")

private fun jsFloatAt(array: JsAny, index: Int): Float = js("array[index]")

private fun jsSetFloatAt(array: JsAny, index: Int, value: Float): Unit = js("{ array[index] = value; }")

// The sound goes through a gain node, whose volume can ramp smoothly on start and stop
private fun startBufferSource(audioContext: JsAny, buffer: JsAny, loop: Boolean, fadeInSeconds: Double): JsAny = js(
    """{
        const source = audioContext.createBufferSource();
        source.buffer = buffer;
        source.loop = loop;
        const gain = audioContext.createGain();
        const now = audioContext.currentTime;
        gain.gain.setValueAtTime(fadeInSeconds > 0 ? 0 : 1, now);
        if (fadeInSeconds > 0) gain.gain.linearRampToValueAtTime(1, now + fadeInSeconds);
        source.connect(gain);
        gain.connect(audioContext.destination);
        source.addEventListener('ended', () => { source.disconnect(); gain.disconnect(); });
        source.start();
        return { source: source, gain: gain };
    }"""
)

private fun onSourceEnded(sound: JsAny, onEnded: () -> Unit): Unit =
    js("{ sound.source.addEventListener('ended', () => onEnded()); }")

// Stopping a sound that has already ended does nothing; its "ended" listener has already disconnected it
private fun stopSource(sound: JsAny, fadeOutSeconds: Double): Unit = js(
    """{
        const now = sound.source.context.currentTime;
        const volume = sound.gain.gain;
        volume.cancelScheduledValues(now);
        volume.setValueAtTime(volume.value, now);
        volume.linearRampToValueAtTime(0, now + fadeOutSeconds);
        sound.source.stop(now + fadeOutSeconds);
    }"""
)
