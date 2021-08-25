package ru.scoltech.openran.speedtest

import android.util.Log
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.net.*
import java.nio.charset.StandardCharsets
import kotlin.jvm.Throws

enum class RequestType { START, STOP }

@Suppress("BlockingMethodInNonBlockingContext") //all right
suspend fun sendGETRequest(
    address: String,
    requestType: RequestType,
    timeout: Long,
    value: String = ""
): String {
    val currentSocketAddress: InetSocketAddress = try {
        parseInetSocketAddress(address, ApplicationConstants.DEFAULT_HTTP_SERVER_PORT)
    } catch (e: UnknownHostException) {
        Log.e("sendGETRequest", "Could not parse address", e)
        return "error"
    }
    val currentAddress = currentSocketAddress.address
    val currentPort = currentSocketAddress.port

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

@Throws(UnknownHostException::class)
fun parseInetSocketAddress(address: String, defaultPort: Int): InetSocketAddress {
    return when {
        !address.contains(':') || (address.first() == '[' && address.last() == ']') ->
            InetSocketAddress(InetAddress.getByName(address), defaultPort)  // IPv4 or IPv6
        address.split(":").size == 2 && address.split(":")[1].isDigitsOnly() -> {
            // IPv4 with port
            val addressAndPort = address.split(":")
            InetSocketAddress(InetAddress.getByName(addressAndPort[0]), addressAndPort[1].toInt())
        }
        address.contains("]:") && address.first() == '[' && address.split("]:").size == 2
                && address.split("]:")[1].isDigitsOnly() -> {
            // IPv6 with port
            val addressAndPort = address.split("]:")
            InetSocketAddress(
                InetAddress.getByName(addressAndPort[0] + ']'),
                addressAndPort[1].toInt()
            )
        }
        else -> throw UnknownHostException("Invalid address format: $address")
    }
}
