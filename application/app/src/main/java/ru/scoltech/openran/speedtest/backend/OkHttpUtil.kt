package ru.scoltech.openran.speedtest.backend

import android.util.Log
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.Response
import java.io.IOException

fun HttpUrl.Builder.addPathSegments(segments: List<String>): HttpUrl.Builder {
    segments.forEach { addPathSegment(it) }
    return this
}

fun HttpUrl.Builder.addQueryParameters(parameters: Map<String, String>): HttpUrl.Builder {
    parameters.forEach { (name, value) -> addQueryParameter(name, value) }
    return this
}

fun Response.closeBody() {
    try {
        body()?.close()
    } catch (e: IOException) {
        Log.e(ServiceApi.LOG_TAG, "Could not close response body", e)
    }
}
