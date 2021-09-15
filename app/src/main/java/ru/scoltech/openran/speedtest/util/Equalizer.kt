package ru.scoltech.openran.speedtest.util

import java.util.function.LongConsumer
import kotlin.jvm.Throws

interface Equalizer<E : Equalizer<E>> {
    fun accept(value: Long): Boolean

    @Throws(NoValueException::class)
    fun getEqualized(): Double

    fun copy(): E

    class NoValueException : Exception()
}
