package ru.scoltech.openran.speedtest.task

import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

/**
 * Task used in [TaskChain].
 * Should be executed in a separate thread.
 *
 * @param T Task argument type
 * @param R Task result type
 */
@FunctionalInterface
interface Task<T, R> {
    /**
     * Creates a [Promise] to execute the task.
     * [Promise.start] should throw [FatalException] if error occurred.
     * Kill action should be registered using [killer], it will be called on task stop.
     * Caller must guarantee that the task is not stopped before or during [Promise.start].
     *
     * @param argument Argument for the task
     * @param killer Killer to register stop task if it is possible.
     * On stop [Promise.onError] should be called.
     * @return Promise accepting task result in [Promise.onSuccess]
     * and error message and/or exception in [Promise.onError]
     */
    fun prepare(
        argument: T,
        killer: TaskKiller
    ): Promise<(R) -> Unit, (String, Exception?) -> Unit>
}
