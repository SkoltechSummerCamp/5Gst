package ru.scoltech.openran.speedtest.task.impl

import com.squareup.okhttp.Call
import ru.scoltech.openran.speedtest.client.balancer.ApiCallback
import ru.scoltech.openran.speedtest.client.balancer.ApiClient
import ru.scoltech.openran.speedtest.client.balancer.model.FiveGstToken

class FiveGstLoginTask : AbstractBalancerRequestTask<BalancerApi, FiveGstToken, BalancerApi>() {
    override fun sendRequest(
        argument: BalancerApi,
        callback: ApiCallback<FiveGstToken>,
    ): Call {
        return argument.loginAsync(callback)
    }

    override fun processApiResult(
        argument: BalancerApi,
        apiResult: FiveGstToken,
    ): BalancerApi {
        val balancerApiClient = ApiClient()
            .setBasePath(argument.apiClient.basePath)
            .setConnectTimeout(argument.apiClient.connectTimeout)
            .setReadTimeout(argument.apiClient.readTimeout)
            .setWriteTimeout(argument.apiClient.writeTimeout)
            .apply {
                setApiKeyPrefix("5Gst")
                setApiKey(apiResult.token)
            }

        return BalancerApi(balancerApiClient)
    }
}
