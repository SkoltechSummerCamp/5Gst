package ru.scoltech.openran.speedtest.task.impl

import com.squareup.okhttp.Call
import ru.scoltech.openran.speedtest.client.balancer.ApiCallback
import ru.scoltech.openran.speedtest.task.impl.model.ApiClientHolder

class FiveGstLogoutTask : AbstractBalancerRequestTask<ApiClientHolder, Void, ApiClientHolder>() {
    override fun sendRequest(
        argument: ApiClientHolder,
        callback: ApiCallback<Void>,
    ): Call {
        return argument.balancerApiClient.logoutAsync(callback)
    }

    override fun processApiResult(
        argument: ApiClientHolder,
        apiResult: Void,
    ): ApiClientHolder {
        return argument
    }
}
