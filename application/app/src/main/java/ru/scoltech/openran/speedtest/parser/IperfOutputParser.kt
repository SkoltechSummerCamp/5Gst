package ru.scoltech.openran.speedtest.parser

import java.io.IOException
import kotlin.jvm.Throws

@FunctionalInterface
interface IperfOutputParser {
    @Throws(IOException::class)
    fun parseSpeed(line: String): Long
}
