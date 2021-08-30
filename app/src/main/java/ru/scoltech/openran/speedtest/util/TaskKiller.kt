package ru.scoltech.openran.speedtest.util

class TaskKiller {
    @Volatile
    private var killTask: (() -> Unit)? = null

    fun register(killTask: () -> Unit) {
        this.killTask = killTask
    }

    fun unregister() {
        killTask = null
    }

    fun kill() {
        killTask?.invoke()
    }
}
