package ru.scoltech.openran.speedtest.task.impl

import io.swagger.client.model.ServerAddressResponse
import ru.scoltech.openran.speedtest.backend.ServiceApi
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

data class StartServiceIperfTask(
    private val balancerApiBuilder: BalancerApiBuilder,
    private val args: String,
) : Task<ServerAddressResponse, ServerAddressResponse> {
    override fun prepare(
        argument: ServerAddressResponse,
        killer: TaskKiller
    ): Promise<(ServerAddressResponse) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
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
