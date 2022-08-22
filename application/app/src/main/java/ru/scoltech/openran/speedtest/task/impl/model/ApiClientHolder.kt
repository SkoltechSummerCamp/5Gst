package ru.scoltech.openran.speedtest.task.impl.model

import ru.scoltech.openran.speedtest.client.service.api.ServiceApi
import ru.scoltech.openran.speedtest.task.impl.BalancerApi

data class ApiClientHolder(
    val balancerApiClient: BalancerApi,
    val serviceApiClient: ServiceApi,
    val iperfAddress: ServerAddress,
)
