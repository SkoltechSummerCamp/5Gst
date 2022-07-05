package ru.scoltech.openran.speedtest.task.impl

import io.swagger.client.model.ServerAddressResponse
import ru.scoltech.openran.speedtest.backend.ServiceApi

data class StopServiceIperfTask(
    private val balancerApiBuilder: BalancerApiBuilder
) : (ServerAddressResponse) -> Unit {
    override fun invoke(p1: ServerAddressResponse) {
        ServiceApi(balancerApiBuilder.httpClient).stopIperf(p1.ip, p1.port)
    }
}
