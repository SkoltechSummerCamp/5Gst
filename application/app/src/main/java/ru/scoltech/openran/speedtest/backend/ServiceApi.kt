package ru.scoltech.openran.speedtest.backend

import android.util.Log
import com.squareup.okhttp.*
import ru.scoltech.openran.speedtest.util.Promise
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ServiceApi(private val httpClient: OkHttpClient) {
    private var startCall: Call? = null
    private val lock = ReentrantLock()

    fun startIperf(
        hostAddress: String,
        port: Int,
        args: String,
    ): Promise<(Response) -> Unit, (Request, IOException) -> Unit> {
        return Promise { onSuccess, onError ->
            lock.withLock {
                cancelStartIperf()
                startCall = buildUrl(
                    hostAddress,
                    port,
                    START_IPERF_PATH_SEGMENTS,
                    mapOf(ARGS_PARAMETER_NAME to args)
                )
                    .buildGetCall()
                    .apply {
                        enqueue(object : Callback {
                            override fun onFailure(request: Request, e: IOException) {
                                onError?.invoke(request, e)
                            }

                            override fun onResponse(response: Response) {
                                onSuccess?.invoke(response)
                                response.closeBody()
                            }
                        })
                    }
            }
        }
    }

    fun cancelStartIperf() {
        lock.withLock {
            startCall?.cancel()
            startCall = null
        }
    }

    fun stopIperf(hostAddress: String, port: Int) {
        buildUrl(hostAddress, port, STOP_IPERF_PATH_SEGMENTS)
            .buildGetCall()
            .enqueue(IgnoreCallback())
    }

    private fun buildUrl(
        hostAddress: String,
        port: Int,
        pathSegments: List<String>,
        queryParameters: Map<String, String> = mapOf(),
    ): HttpUrl {
        return HttpUrl.Builder()
            .scheme("http")
            .host(hostAddress)
            .port(port)
            .addPathSegments(pathSegments)
            .addQueryParameters(queryParameters)
            .build()
            .also { Log.d(LOG_TAG, "Sending request to $it") }
    }

    private fun HttpUrl.buildGetCall(): Call {
        val request = Request.Builder()
            .url(this)
            .get()
            .build()
        request.body()
        return httpClient.newCall(request)
    }

    private class IgnoreCallback : Callback {
        override fun onFailure(request: Request, e: IOException) {}

        override fun onResponse(response: Response) {
            response.closeBody()
        }
    }

    companion object {
        const val LOG_TAG = "ServiceApi"
        val STOP_IPERF_PATH_SEGMENTS = listOf("stop-iperf")
        val START_IPERF_PATH_SEGMENTS = listOf("start-iperf")
        const val ARGS_PARAMETER_NAME = "args"
    }
}
