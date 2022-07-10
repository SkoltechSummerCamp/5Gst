package ru.scoltech.openran.speedtest.util

class Promise<S, E>(private val startInner: (S?, E?) -> Unit) {
    private var onSuccess: S? = null
    private var onError: E? = null

    fun onSuccess(onSuccess: S): Promise<S, E> {
        this.onSuccess = onSuccess
        return this
    }

    fun onError(onError: E): Promise<S, E> {
        this.onError = onError
        return this
    }

    fun start() {
        startInner(onSuccess, onError)
    }
}
