package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ModemTest {

    private val joinCode = "4821".encodeToByteArray()

    @Test
    fun `crc16 matches the standard check value`() {
        assertThat(crc16("123456789".encodeToByteArray())).isEqualTo(0x29B1)
    }

    @Test
    fun `a 4 byte message takes 14 symbols after the chirp and gap`() {
        // length + 4 bytes + 2 check bytes = 7 bytes = 14 symbols
        assertThat(transmit(joinCode).size).isEqualTo(2_400 + SYMBOL_SIZE + 14 * SYMBOL_SIZE)
    }

    @Test
    fun `every symbol fades to silence at its edges so tone changes don't tick`() {
        val sound = transmit(joinCode)
        val dataStart = 2_400 + SYMBOL_SIZE

        (0..14).forEach { boundary ->
            val at = dataStart + boundary * SYMBOL_SIZE
            if (at < sound.size) assertThat(sound[at]).isCloseTo(0f, 0.001f)
            assertThat(sound[at - 1]).isCloseTo(0f, 0.01f)
        }
    }

    @Test
    fun `a message comes through unchanged`() {
        val recording = silence(0.3) + transmit(joinCode) + silence(0.3)

        assertThat(receivedBytes(receive(recording))).isEqualTo(joinCode.toList())
    }

    @Test
    fun `a quiet message comes through noise`() {
        val random = Random(3)
        val sound = silence(0.3) + transmit(joinCode).map { it * 0.2f }.toFloatArray() + silence(0.3)
        val recording = FloatArray(sound.size) { sound[it] + (random.nextFloat() * 2 - 1) * 0.1f }

        assertThat(receivedBytes(receive(recording))).isEqualTo(joinCode.toList())
    }

    @Test
    fun `a message comes through a room's echo`() {
        // A wall bounce 10 ms later at 40% loudness
        val sound = silence(0.3) + transmit(joinCode) + silence(0.3)
        val recording = FloatArray(sound.size) { n -> sound[n] + if (n >= 480) 0.4f * sound[n - 480] else 0f }

        assertThat(receivedBytes(receive(recording))).isEqualTo(joinCode.toList())
    }

    @Test
    fun `every byte value comes through`() {
        val everyByte = ByteArray(MAX_MESSAGE_BYTES) { it.toByte() }
        val recording = silence(0.1) + transmit(everyByte) + silence(0.1)

        assertThat(receivedBytes(receive(recording))).isEqualTo(everyByte.toList())
    }

    @Test
    fun `an empty message comes through`() {
        val recording = silence(0.1) + transmit(ByteArray(0)) + silence(0.1)

        assertThat(receivedBytes(receive(recording))).isEqualTo(emptyList<Byte>())
    }

    @Test
    fun `a wrong tone is caught as garbled`() {
        val sound = silence(0.1) + transmit(joinCode) + silence(0.1)
        // Silence one symbol in the middle of the message, so its tone is lost
        val symbol5 = sampleCountFor(0.1) + 2_400 + SYMBOL_SIZE + 5 * SYMBOL_SIZE
        sound.fill(0f, symbol5, symbol5 + SYMBOL_SIZE)

        assertThat(receive(sound)).isNotNull().isInstanceOf(Reception.Garbled::class)
    }

    @Test
    fun `nothing is received from silence`() {
        assertThat(receive(silence(1.0))).isNull()
    }

    @Test
    fun `nothing is received from noise alone`() {
        val random = Random(5)
        val noise = FloatArray(sampleCountFor(3.0)) { (random.nextFloat() * 2 - 1) * 0.3f }

        assertThat(receive(noise)).isNull()
    }

    @Test
    fun `a message is found deep into a long recording`() {
        // Starts several search windows in, and right across a window boundary
        val recording = FloatArray(65_536 * 3 - 1_000) + transmit(joinCode) + silence(0.1)

        assertThat(receivedBytes(receive(recording))).isEqualTo(joinCode.toList())
    }

    @Test
    fun `a message cut off by the end of the recording waits for more`() {
        val sound = silence(0.1) + transmit(joinCode)

        assertThat(receive(sound.copyOfRange(0, sound.size - SYMBOL_SIZE))).isNull()
    }

    @Test
    fun `two messages are received one after the other`() {
        val second = "hi".encodeToByteArray()
        val recording = silence(0.1) + transmit(joinCode) + silence(0.2) + transmit(second) + silence(0.1)

        val first = checkNotNull(receive(recording))
        assertThat(receivedBytes(first)).isEqualTo(joinCode.toList())
        assertThat(receivedBytes(receive(recording, from = first.endsAt))).isEqualTo(second.toList())
    }

    @Test
    fun `more than 255 bytes is refused`() {
        assertFailsWith<IllegalArgumentException> { transmit(ByteArray(256)) }
    }

    private fun silence(seconds: Double) = FloatArray(sampleCountFor(seconds))

    private fun receivedBytes(reception: Reception?): List<Byte>? = (reception as? Reception.Message)?.bytes?.toList()
}
