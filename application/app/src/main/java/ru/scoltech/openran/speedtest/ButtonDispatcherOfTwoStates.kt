package ru.scoltech.openran.speedtest;

import android.app.Activity
import android.widget.Button
import java.util.concurrent.atomic.AtomicBoolean

class ButtonDispatcherOfTwoStates(
    private val button: Button,
    private val activityOfButton: Activity,
    private val secondTextValue: String
) {
    private var inFirstState = true
    var firstAction: () -> (Unit) = {}
    var secondAction: () -> (Unit) = {}
    var firstTextValue = button.text.toString()
    var isBlocked = AtomicBoolean(false)

    init {
        button.setOnClickListener {
            if (!isBlocked.get()) {
                if (inFirstState) {
                    firstAction()
                } else {
                    secondAction()
                }
                changeState()
            }
        }
    }

    fun changeState() {
        inFirstState = if (inFirstState) {
            activityOfButton.runOnUiThread { button.text = secondTextValue }
            false
        } else {
            activityOfButton.runOnUiThread { button.text = firstTextValue }
            true
        }
    }

    fun lock() {
        isBlocked.set(true)
    }

    fun unlock() {
        isBlocked.set(false)
    }
}
