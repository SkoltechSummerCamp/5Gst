package ru.scoltech.openran.speedtest.task.impl

import io.swagger.client.model.ServerAddr
import ru.scoltech.openran.speedtest.backend.ServiceApi
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

data class StartServiceIperfTask(
    private val balancerApiBuilder: BalancerApiBuilder,
    private val args: String,
) : Task<ServerAddr, ServerAddr> {
    override fun prepare(
        argument: ServerAddr,
        killer: TaskKiller
    ): Promise<(ServerAddr) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
        val serviceApi = ServiceApi(balancerApiBuilder.httpClient)

        killer.register(serviceApi::cancelStartIperf)
        // TODO check request/response body for memory leaks
        serviceApi.startIperf(argument.ip, argument.port, args)
            .onSuccess { onSuccess?.invoke(argument) }
            .onError { _, e ->
                onError?.invoke("Could not connect to the server", e)
            }
            .start()
    }
}
