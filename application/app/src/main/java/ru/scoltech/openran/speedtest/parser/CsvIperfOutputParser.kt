package ru.scoltech.openran.speedtest.parser

import com.opencsv.CSVParser
import java.io.IOException
import kotlin.jvm.Throws

class CsvIperfOutputParser : IperfOutputParser {
    @Throws(IOException::class)
    override fun parseSpeed(line: String): Long {
        return CSVParser().parseLine(line)[8].toLong()
    }
}
