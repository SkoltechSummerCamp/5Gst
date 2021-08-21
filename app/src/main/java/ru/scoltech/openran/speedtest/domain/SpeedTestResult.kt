package ru.scoltech.openran.speedtest.domain

import java.util.*

data class SpeedTestResult(
    val uploadSpeed: Long,
    val downloadSpeed: Long,
    val ping: Long,
    val creationTime: Date,
    val serverAddress: String,
    val description: String,
    val id: Long = 0,
)
