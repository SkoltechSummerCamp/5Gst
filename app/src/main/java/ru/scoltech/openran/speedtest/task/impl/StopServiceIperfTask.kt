package ru.scoltech.openran.speedtest.task.impl

import io.swagger.client.model.ServerAddr
import ru.scoltech.openran.speedtest.backend.ServiceApi

data class StopServiceIperfTask(
    private val balancerApiBuilder: BalancerApiBuilder
) : (ServerAddr) -> Unit {
    override fun invoke(p1: ServerAddr) {
        ServiceApi(balancerApiBuilder.httpClient).stopIperf(p1.ip, p1.port)
    }
}
