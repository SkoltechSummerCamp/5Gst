package ru.scoltech.openran.speedtest.task.impl

import ru.scoltech.openran.speedtest.backend.ServiceApi
import ru.scoltech.openran.speedtest.client.balancer.model.ServerAddressResponse

data class StopServiceIperfTask(
    private val balancerApiBuilder: BalancerApiBuilder
) : (ServerAddressResponse) -> Unit {
    override fun invoke(p1: ServerAddressResponse) {
        ServiceApi(balancerApiBuilder.httpClient).stopIperf(p1.ip, p1.port)
    }
}
