package com.eeseka.nimbat.core.domain.audio.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Regroups blocks of any size into blocks of exactly [size] samples, each starting [step] samples after the last.
 * A [step] smaller than [size] makes the blocks overlap.
 */
fun Flow<FloatArray>.inBlocksOf(size: Int, step: Int = size): Flow<FloatArray> = flow {
    require(step in 1..size) { "step must be between 1 and $size, was $step" }
    val pending = FloatArray(size)
    var filled = 0
    collect { samples ->
        for (sample in samples) {
            pending[filled++] = sample
            if (filled == size) {
                emit(pending.copyOf())
                pending.copyInto(pending, destinationOffset = 0, startIndex = step)
                filled = size - step
            }
        }
    }
}
