package ru.scoltech.openran.speedtest.parser

import java.io.IOException
import kotlin.jvm.Throws

class MultithreadedIperfOutputParser : IperfOutputParser {
    @Throws(BadFormatException::class)
    override fun parseSpeed(line: String): Long {
        return regex.matchEntire(line)?.let {
            it.groupValues[2].toLong()
        } ?: throw BadFormatException("Output does not match the pattern $regex")
    }

    class BadFormatException(message: String) : IOException(message)

    companion object {
        // [SUM-%d] %4.2f-%4.2f sec  %d Bytes  %d bits/sec ...
        private val regex = Regex(
            "^\\[SUM-\\d+]\\s+\\d{1,4}\\.\\d{2}-\\d{1,4}\\.\\d{2}\\s+sec" +
                    "\\s+\\d+(\\.\\d+)?\\s+Bytes\\s+(\\d+)(\\.\\d+)?\\s+bits/sec.*$"
        )
    }
}
