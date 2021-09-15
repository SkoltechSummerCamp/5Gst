package ru.scoltech.openran.speedtest.util

import java.util.*

class SkipThenAverageEqualizer(
    private val skipAmount: Int,
    private val maxStoring: Int
) : Equalizer<SkipThenAverageEqualizer> {
    private val valueQueue: Queue<Long> = ArrayDeque(maxStoring)
    private var skipped = 0

    override fun getEqualized(): Double {
        return if (valueQueue.size == maxStoring) {
            valueQueue.average()
        } else {
            throw Equalizer.NoValueException()
        }
    }

    override fun copy(): SkipThenAverageEqualizer {
        return SkipThenAverageEqualizer(skipAmount, maxStoring).apply {
            valueQueue.addAll(this@SkipThenAverageEqualizer.valueQueue)
        }
    }

    override fun accept(value: Long): Boolean {
        if (skipped < skipAmount) {
            skipped++
            return false
        }
        if (valueQueue.size == maxStoring) {
            valueQueue.remove()
        }
        return valueQueue.add(value)
    }
}
