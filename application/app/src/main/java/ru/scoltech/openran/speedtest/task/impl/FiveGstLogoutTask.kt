package ru.scoltech.openran.speedtest.task.impl

import com.squareup.okhttp.Call
import ru.scoltech.openran.speedtest.client.balancer.ApiCallback
import ru.scoltech.openran.speedtest.task.impl.model.ApiClientHolder

class FiveGstLogoutTask : AbstractBalancerRequestTask<BalancerApi, Void?, BalancerApi>() {
    override fun sendRequest(
        argument: BalancerApi,
        callback: ApiCallback<Void?>,
    ): Call {
        return argument.logoutAsync(callback)
    }

    override fun processApiResult(
        argument: BalancerApi,
        apiResult: Void?,
    ): BalancerApi {
        return argument
    }
}
