package ru.scoltech.openran.speedtest

import android.util.Log
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.net.*
import java.nio.charset.StandardCharsets

enum class RequestType { START, STOP }

@Suppress("BlockingMethodInNonBlockingContext") //all right
suspend fun sendGETRequest(
    address: String,
    requestType: RequestType,
    timeout: Long,
    value: String = ""
): String {
    var currentPort = ApplicationConstants.HTTP_SERVER_PORT;

    var currentAddress: InetAddress = try {
        when {
            !address.contains(':') || (address.first() == '[' && address.last() == ']') ->
                InetAddress.getByName(address) //IPv4 or IPv6
            address.split(":").size == 2 && address.split(":")[1].isDigitsOnly() -> {
                val addressAndPort = address.split(":")
                currentPort = addressAndPort[1].toInt()
                InetAddress.getByName(addressAndPort[0])
            }//IPv4 with port
            address.contains("]:") && address.first() == '[' && address.split("]:").size == 2
                    && address.split("]:")[1].isDigitsOnly() -> {
                val addressAndPort = address.split("]:")
                currentPort = addressAndPort[1].toInt()
                InetAddress.getByName(addressAndPort[0] + ']')
            }//IPv6 with port
            else -> return "error"
        }
    } catch (e: UnknownHostException) {
        Log.e("sendGETRequest", e.message!!)
        return "error"
    }
    val url = when (requestType) {
        RequestType.START ->
            "http://${currentAddress.hostAddress}:$currentPort/start-iperf?args=${
                URLEncoder.encode(
                    value,
                    StandardCharsets.UTF_8.toString()
                )
            }"
        RequestType.STOP ->
            "http://${currentAddress.hostAddress}:$currentPort/stop-iperf"
    }
    val channel = Channel<String>()
    val connection = try {
        Log.d("sendGETRequest", url)
        URL(url).openConnection() as HttpURLConnection
    } catch (e: IOException) {
        return "error"
    }
    CoroutineScope(Dispatchers.IO).launch {
        try {
            channel.trySend(String(connection.inputStream.readBytes(), StandardCharsets.UTF_8))
        } catch (e: IOException) {
            channel.trySend("error")
        } finally {
            connection.disconnect()
        }
    }
    CoroutineScope(Dispatchers.IO).launch {
        delay(timeout)
        channel.trySend("error")
        connection.disconnect()
    }
    return channel.receive()
}