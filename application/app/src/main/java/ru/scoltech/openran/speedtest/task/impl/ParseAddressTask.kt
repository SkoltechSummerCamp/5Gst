package ru.scoltech.openran.speedtest.task.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.scoltech.openran.speedtest.backend.parseInetSocketAddress
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.task.impl.model.ServerAddress
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller
import java.net.UnknownHostException

class ParseAddressTask : Task<String, ServerAddress> {
    override fun prepare(
        argument: String,
        killer: TaskKiller
    ): Promise<(ServerAddress) -> Unit, (String, Exception?) -> Unit> {
        return Promise { onSuccess, onError ->
            CoroutineScope(Dispatchers.IO).launch {
                val address = try {
                    // TODO default port
                    // TODO dont resolve address
                    // Dispatchers.IO scope is designed for blocking calls
                    @Suppress("BlockingMethodInNonBlockingContext")
                    parseInetSocketAddress(argument, 80)
                } catch (e: UnknownHostException) {
                    onError?.invoke("Unknown host $argument", e)
                    return@launch
                }
                val host = address.address.hostAddress
                    ?: kotlin.run {
                        onError?.invoke("Parsed address $address has no hostAddress", null)
                        return@launch
                    }
                onSuccess?.invoke(ServerAddress(host, address.port))
            }
        }
    }
}
