package ru.scoltech.openran.speedtest.task.impl

import com.squareup.okhttp.Call
import com.squareup.okhttp.HttpUrl
import ru.scoltech.openran.speedtest.client.balancer.ApiCallback
import ru.scoltech.openran.speedtest.client.balancer.model.ServerAddressResponse
import ru.scoltech.openran.speedtest.client.service.ApiClient
import ru.scoltech.openran.speedtest.client.service.api.ServiceApi
import ru.scoltech.openran.speedtest.task.impl.model.ApiClientHolder
import ru.scoltech.openran.speedtest.task.impl.model.ServerAddress

class ObtainServiceAddressTask(
    private val serviceConnectTimeout: Int,
    private val serviceReadTimeout: Int,
    private val serviceWriteTimeout: Int,
) : AbstractBalancerRequestTask<BalancerApi, ServerAddressResponse, ApiClientHolder>() {
    override fun sendRequest(
        argument: BalancerApi,
        callback: ApiCallback<ServerAddressResponse>,
    ): Call {
        return argument.acquireServiceAsync(callback)
    }

    override fun processApiResult(
        argument: BalancerApi,
        apiResult: ServerAddressResponse,
    ): ApiClientHolder {
        val serviceAddress = HttpUrl.Builder()
            .scheme("http")  // TODO https
            .host(apiResult.ip)
            .port(apiResult.port)
            .build()
            .toString()
            .dropLast(1)  // drops trailing '/'

        val serviceApiClient = ApiClient()
            .setBasePath(serviceAddress)
            .setConnectTimeout(serviceConnectTimeout)
            .setReadTimeout(serviceReadTimeout)
            .setWriteTimeout(serviceWriteTimeout)

        return ApiClientHolder(
            argument,
            ServiceApi(serviceApiClient),
            ServerAddress(apiResult.ip, apiResult.portIperf),
        )
    }
}
