package ru.scoltech.openran.speedtest.task.impl

import ru.scoltech.openran.speedtest.task.impl.model.ApiClientHolder

class StopServiceIperfTask : (ApiClientHolder) -> Unit {
    override fun invoke(p1: ApiClientHolder) {
        p1.serviceApiClient.stopIperf()
    }
}
