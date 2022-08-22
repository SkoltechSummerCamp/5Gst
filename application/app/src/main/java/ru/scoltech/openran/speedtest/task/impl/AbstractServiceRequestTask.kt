package ru.scoltech.openran.speedtest.task.impl

import com.squareup.okhttp.Call
import ru.scoltech.openran.speedtest.client.service.ApiCallback
import ru.scoltech.openran.speedtest.client.service.ApiException
import ru.scoltech.openran.speedtest.task.FatalException
import ru.scoltech.openran.speedtest.task.Task
import ru.scoltech.openran.speedtest.util.Promise
import ru.scoltech.openran.speedtest.util.TaskKiller

abstract class AbstractServiceRequestTask<ARGUMENT, API_RESULT, RESULT> : Task<ARGUMENT, RESULT> {
    override fun prepare(
        argument: ARGUMENT,
        killer: TaskKiller
    ): Promise<(RESULT) -> Unit, (String, Exception?) -> Unit> = Promise { onSuccess, onError ->
        val call = try {
            sendRequest(argument, ApiCallbackImpl(onSuccess, onError, argument))
        } catch (e: ApiException) {
            throw FatalException("Could not create api call", e)
        }

        killer.register(call::cancel)
    }

    abstract fun sendRequest(argument: ARGUMENT, callback: ApiCallback<API_RESULT>): Call

    abstract fun processApiResult(argument: ARGUMENT, apiResult: API_RESULT): RESULT

    private inner class ApiCallbackImpl(
        private val onSuccess: ((RESULT) -> Unit)?,
        private val onError: ((String, ApiException?) -> Unit)?,
        private val argument: ARGUMENT,
    ) : ApiCallback<API_RESULT> {
        override fun onFailure(
            e: ApiException?,
            statusCode: Int,
            responseHeaders: MutableMap<String, MutableList<String>>?
        ) {
            val statusCodeMessage = if (statusCode != 0) {
                " (status code = $statusCode)"
            } else {
                ""
            }
            onError?.invoke("Could not connect to balancer$statusCodeMessage", e)
        }

        override fun onSuccess(
            result: API_RESULT,
            statusCode: Int,
            responseHeaders: MutableMap<String, MutableList<String>>?
        ) {
            onSuccess?.invoke(processApiResult(argument, result))
        }

        override fun onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean) {
            // no operations
        }

        override fun onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
            // no operations
        }

    }
}