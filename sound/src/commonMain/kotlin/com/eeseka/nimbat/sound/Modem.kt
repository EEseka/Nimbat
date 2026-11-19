package com.eeseka.nimbat.sound

/**
 * Samples per symbol: 2,048 ≈ 43 ms. Each symbol is one tone carrying 4 bits, so a byte takes two symbols
 * and the modem sends about 11.7 bytes a second.
 */
const val SYMBOL_SIZE = 2_048

/** The most bytes one message can carry: its length goes in a single byte. */
const val MAX_MESSAGE_BYTES = 255

/** The chirp that announces a message. A wider sweep than the ranging chirps, so the two are never confused. */
val MESSAGE_START = ChirpTemplate(17_500.0, 20_500.0, 0.05)

/**
 * How clear a start chirp must be to count. Without a chirp, a recording's biggest bump of noise still
 * stands about 12–14 dB above its typical score; real chirps measured 50 dB and up.
 */
const val MIN_START_CLARITY_DB = 20.0

/**
 * The stretch searched for a start chirp at a time: 65,536 samples ≈ 1.4 s. Searching window by window
 * keeps each FFT small however long the recording is, and stops at the first message.
 */
private const val SEARCH_WINDOW = 65_536

/** Silence between the start chirp and the first symbol, so the chirp's echoes die down first. */
private const val GAP_SIZE = SYMBOL_SIZE

/** Tones per symbol bank: 16, one for each value 4 bits can hold (0–15). */
private const val TONES_PER_BANK = 16

/**
 * The lowest tone, as an FFT slot of one symbol. Each slot is 48,000 ÷ 2,048 ≈ 23.4 Hz wide, so slot 750 is
 * ≈ 17,578 Hz. Every tone sits exactly on a slot, so it fits a whole number of cycles in one symbol and the FFT
 * sees it as one clean tone.
 */
private const val FIRST_SLOT = 750

/** Each symbol fades in and out over 240 samples (5 ms); a sudden jump to the next tone ticks audibly. */
private const val SYMBOL_FADE_SIZE = 240

/** Slots between neighbouring tones: 4 ≈ 94 Hz, so a tone stays in its own slot even if the timing is off a bit. */
private const val SLOT_STEP = 4

/**
 * What a recording held after a start chirp: a [Message] whose check matched, or something [Garbled]
 * (noise or a missed tone changed it on the way). [endsAt] is the sample just after it, where to look next.
 */
sealed interface Reception {
    val endsAt: Int

    class Message(val bytes: ByteArray, override val endsAt: Int) : Reception

    data class Garbled(override val endsAt: Int) : Reception
}

/**
 * The sound that carries [bytes]: the start chirp, a short silence, then two tones per byte.
 *
 * On the way, the bytes are wrapped as `length, bytes…, check (2 bytes)`. The length tells the listener when
 * to stop; the check ([crc16]) tells it whether anything changed on the way.
 *
 * Even symbols use the lower 16 tones and odd symbols the upper 16. A room's echo of one symbol spills into
 * the next, but it lands in the other bank, where the listener isn't looking.
 */
fun transmit(bytes: ByteArray): FloatArray {
    require(bytes.size <= MAX_MESSAGE_BYTES) { "at most $MAX_MESSAGE_BYTES bytes, was ${bytes.size}" }
    val frame = frame(bytes)
    val chirp = MESSAGE_START.wave
    val dataStart = chirp.size + GAP_SIZE
    val samples = FloatArray(dataStart + frame.size * 2 * SYMBOL_SIZE)
    chirp.copyInto(samples)

    frame.toNibbles().forEachIndexed { symbol, nibble ->
        val tone = loopingTone(cycles = slotFor(symbol, nibble), sampleCount = SYMBOL_SIZE)
        fadeEdges(samples = tone, fadeSamples = SYMBOL_FADE_SIZE)
        tone.copyInto(samples, destinationOffset = dataStart + symbol * SYMBOL_SIZE)
    }
    return samples
}

/**
 * Listens for the first message starting at or after sample [from] in [recording].
 * Returns null when no start chirp is heard, or when the recording ends before the message does
 * (keep recording and try again). A misheard length byte can make it wait longer than the real message,
 * at most as long as the longest one (≈ 22 s), before the check fails and it reports [Reception.Garbled].
 */
fun receive(recording: FloatArray, from: Int = 0): Reception? {
    val start = findStart(recording, from) ?: return null
    val dataStart = start + MESSAGE_START.wave.size + GAP_SIZE
    fun symbolEnd(symbols: Int) = dataStart + symbols * SYMBOL_SIZE

    if (symbolEnd(2) > recording.size) return null
    val length = (readNibble(recording, dataStart, 0) shl 4) or readNibble(recording, dataStart, 1)
    val frameSymbols = (1 + length + 2) * 2
    val endsAt = symbolEnd(frameSymbols)
    if (endsAt > recording.size) return null

    val frame = ByteArray(frameSymbols / 2) { i ->
        val high = readNibble(recording, dataStart, 2 * i)
        val low = readNibble(recording, dataStart, 2 * i + 1)
        ((high shl 4) or low).toByte()
    }
    val body = frame.copyOfRange(0, frame.size - 2)
    val sentCheck = ((frame[frame.size - 2].toInt() and 0xFF) shl 8) or (frame[frame.size - 1].toInt() and 0xFF)

    return if (crc16(body) == sentCheck) {
        Reception.Message(bytes = body.copyOfRange(1, body.size), endsAt = endsAt)
    } else {
        Reception.Garbled(endsAt)
    }
}

/**
 * A 16-bit check worked out from [bytes] (CRC-16/CCITT). The sender appends it; the listener works it out
 * again from what it heard. Any single wrong tone, or any run of wrong tones up to 4 symbols long,
 * always changes it, so a mismatch means the message is garbled.
 */
fun crc16(bytes: ByteArray): Int {
    var crc = 0xFFFF
    for (byte in bytes) {
        crc = crc xor ((byte.toInt() and 0xFF) shl 8)
        repeat(8) {
            crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
        }
        crc = crc and 0xFFFF
    }
    return crc
}

/**
 * Where the first clear start chirp at or after [from] begins, searching one [SEARCH_WINDOW] at a time.
 * Neighbouring windows overlap by a chirp's length, so a chirp across a boundary is whole in the next one.
 */
private fun findStart(recording: FloatArray, from: Int): Int? {
    val chirpSize = MESSAGE_START.wave.size
    var windowStart = from.coerceAtLeast(0)
    while (windowStart + chirpSize <= recording.size) {
        val windowEnd = minOf(recording.size, windowStart + SEARCH_WINDOW)
        val hit = Finder(recording.copyOfRange(windowStart, windowEnd), MESSAGE_START).find()
        if (hit != null && hit.clarityDb >= MIN_START_CLARITY_DB) return windowStart + hit.sample
        if (windowEnd == recording.size) return null
        windowStart = windowEnd - chirpSize + 1
    }
    return null
}

/** Wraps [bytes] for sending: `length, bytes…, check high byte, check low byte`. */
private fun frame(bytes: ByteArray): ByteArray {
    val body = byteArrayOf(bytes.size.toByte()) + bytes
    val check = crc16(body)
    return body + byteArrayOf((check shr 8).toByte(), check.toByte())
}

/** Splits each byte into two 4-bit values (nibbles), high first: 0x4B → 4, 11. */
private fun ByteArray.toNibbles(): List<Int> = flatMap { byte ->
    val value = byte.toInt() and 0xFF
    listOf(value shr 4, value and 0xF)
}

/** The FFT slot of the tone for [nibble] in symbol number [symbol]: lower bank for even symbols, upper for odd. */
private fun slotFor(symbol: Int, nibble: Int): Int {
    val bank = symbol % 2
    return FIRST_SLOT + (bank * TONES_PER_BANK + nibble) * SLOT_STEP
}

/** Unmixes symbol number [symbol] and returns which of its bank's 16 tones is loudest. */
private fun readNibble(recording: FloatArray, dataStart: Int, symbol: Int): Int {
    val offset = dataStart + symbol * SYMBOL_SIZE
    val re = DoubleArray(SYMBOL_SIZE) { recording[offset + it].toDouble() }
    val im = DoubleArray(SYMBOL_SIZE)
    fft(re, im)
    return (0 until TONES_PER_BANK).maxBy { nibble ->
        val slot = slotFor(symbol, nibble)
        re[slot] * re[slot] + im[slot] * im[slot]
    }
}
