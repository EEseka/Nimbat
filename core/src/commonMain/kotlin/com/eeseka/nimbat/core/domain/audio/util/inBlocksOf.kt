package com.eeseka.nimbat.core.domain.audio.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Regroups blocks of any size into blocks of exactly [size] samples. */
fun Flow<FloatArray>.inBlocksOf(size: Int): Flow<FloatArray> = flow {
    val pending = FloatArray(size)
    var filled = 0
    collect { samples ->
        for (sample in samples) {
            pending[filled++] = sample
            if (filled == size) {
                emit(pending.copyOf())
                filled = 0
            }
        }
    }
}
