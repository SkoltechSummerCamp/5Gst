package ru.scoltech.openran.speedtest.util

import kotlinx.coroutines.*

class IdleTaskKiller {
    @Volatile
    private var updatedMoment: Long = 0L

    private var killerJob: Job? = null

    @Synchronized
    suspend fun register(allowedIdleMillis: Long, killTask: () -> Unit) {
        unregister()
        updatedMoment = System.currentTimeMillis()
        killerJob = CoroutineScope(Dispatchers.Default).launch {
            var toWait = allowedIdleMillis
            do {
                delay(toWait)
                toWait = allowedIdleMillis - (System.currentTimeMillis() - updatedMoment)
            } while (toWait > 0)
            killTask()
        }
    }

    fun registerBlocking(allowedIdleMillis: Long, killTask: () -> Unit) {
        runBlocking {
            register(allowedIdleMillis, killTask)
        }
    }

    fun updateTaskState() {
        updatedMoment = System.currentTimeMillis()
    }

    @Synchronized
    suspend fun unregister() {
        killerJob?.cancelAndJoin()
        killerJob = null
    }

    fun unregisterBlocking() {
        runBlocking {
            unregister()
        }
    }
}
