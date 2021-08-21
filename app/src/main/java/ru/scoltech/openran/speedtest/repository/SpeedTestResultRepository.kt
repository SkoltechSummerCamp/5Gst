package ru.scoltech.openran.speedtest.repository

import ru.scoltech.openran.speedtest.domain.SpeedTestResult

interface SpeedTestResultRepository {
    fun save(result: SpeedTestResult)
    fun findAllByPageAndSizeOrderedById(page: Long, size: Long): List<SpeedTestResult>
    fun findAll(): List<SpeedTestResult>
}
