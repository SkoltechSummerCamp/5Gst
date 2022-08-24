package ru.scoltech.openran.speedtest.task.impl

import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

class DelayTask<T>(private val delayMillis: Long) : Task<T, T> {
    override fun prepare(
        argument: T,
        killer: TaskKiller
    ): Promise<(T) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
        val delayCoroutine = CoroutineScope(Dispatchers.Default).launch {
            try {
                delay(delayMillis)
                onSuccess?.invoke(argument)
            } catch (e: CancellationException) {
                onError?.invoke("Cancelled", e)
                return@launch
            }
        }
        killer.register { delayCoroutine.cancel() }
    }
}
