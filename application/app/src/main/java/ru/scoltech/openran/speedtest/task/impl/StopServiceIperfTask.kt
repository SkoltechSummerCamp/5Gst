package ru.scoltech.openran.speedtest.task.impl

import com.squareup.okhttp.Call
import ru.scoltech.openran.speedtest.client.service.ApiCallback
import ru.scoltech.openran.speedtest.task.impl.model.ApiClientHolder

class StopServiceIperfTask : AbstractServiceRequestTask<ApiClientHolder, Void?, ApiClientHolder>() {
    override fun sendRequest(argument: ApiClientHolder, callback: ApiCallback<Void?>): Call {
        return argument.serviceApiClient.stopIperfAsync(callback)
    }

    override fun processApiResult(argument: ApiClientHolder, apiResult: Void?): ApiClientHolder {
        return argument
    }
}
