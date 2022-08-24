package ru.scoltech.openran.speedtest.task

import ru.scoltech.openran.speedtest.util.TaskKiller
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TaskChain<S : Any?>(
    private val tasks: List<Task<Any?, Any?>>,
    private val onSuccess: (Any?) -> Unit,
    private val onStop: () -> Unit,
    private val onFatalError: (String, Exception?) -> Unit,
) {
    private val lock = ReentrantLock()
    private var stopped = false
    private val taskKiller = TaskKiller()

    fun start(initialValue: S) {
        prepare(initialValue, tasks)
    }

    private fun prepare(argument: Any?, tasks: List<Task<Any?, Any?>>) {
        lock.withLock {
            if (tasks.isEmpty()) {
                onSuccess(argument)
                return
            } else if (stopped) {
                onStop()
                return
            }

            try {
                tasks[0].prepare(argument, taskKiller)
                    .onError(::onTaskError)
                    .onSuccess { prepare(it, tasks.subList(1, tasks.size)) }
                    .start()
            } catch (e: FatalException) {
                onTaskError(e.message!!, e.cause as? Exception)
            }
        }
    }

    private fun onTaskError(message: String, exception: Exception?) {
        lock.withLock {
            if (stopped) {
                onStop()
            } else {
                onFatalError(message, exception)
            }
        }
    }

    fun stop() {
        lock.withLock {
            stopped = true
            taskKiller.kill()
        }
    }
}
