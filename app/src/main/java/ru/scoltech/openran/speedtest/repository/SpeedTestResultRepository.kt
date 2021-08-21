package ru.scoltech.openran.speedtest.repository

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteException
import ru.scoltech.openran.speedtest.domain.SpeedTestResult
import kotlin.jvm.Throws

interface SpeedTestResultRepository {
    fun save(result: SpeedTestResult)
    fun findAllByPageAndSizeOrderedById(page: Long, size: Long): List<SpeedTestResult>
    fun findAll(): List<SpeedTestResult>

    companion object {
        @Volatile
        private lateinit var INSTANCE: SpeedTestResultRepository

        @Throws(SQLException::class, SQLiteException::class)
        fun getInstance(context: Context): SpeedTestResultRepository {
            if (!this::INSTANCE.isInitialized) {
                synchronized(this) {
                    if (!this::INSTANCE.isInitialized) {
                        INSTANCE = SpeedTestResultRepositoryImpl(context)
                    }
                }
            }
            return INSTANCE
        }
    }
}
