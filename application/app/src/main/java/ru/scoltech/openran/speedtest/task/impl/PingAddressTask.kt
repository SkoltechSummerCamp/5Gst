package ru.scoltech.openran.speedtest.task.impl

import ru.scoltech.openran.speedtest.backend.IcmpPinger
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.task.impl.model.ServerAddress
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

class PingAddressTask(
    private val timeout: Long,
    private val onPingUpdate: (Long) -> Unit,
) : Task<ServerAddress, ServerAddress> {
    override fun prepare(
        argument: ServerAddress,
        killer: TaskKiller,
    ): Promise<(ServerAddress) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
        // TODO support many addresses
        val icmpPinger = IcmpPinger()

        killer.register { icmpPinger.stop() }
        icmpPinger.pingOnce(argument.host, timeout)
            .onSuccess {
                onPingUpdate(it)
                onSuccess?.invoke(argument)
            }
            .onError { onError?.invoke("Could not get service ping", it) }
            .start()
    }
}
