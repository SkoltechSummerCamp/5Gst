package ru.scoltech.openran.speedtest;

import android.app.Activity
import android.widget.Button
import java.util.concurrent.atomic.AtomicBoolean

class ButtonDispatcherOfTwoStates(
    button: Button,
    activityOfButton: Activity,
    secondTextValue: String
) {
    private var inFirstState = true
    var firstAction: () -> (Unit) = {}
    var secondAction: () -> (Unit) = {}
    var firstTextValue = button.text.toString()
    var isBlocked = AtomicBoolean(false)

    init {
        button.setOnClickListener {
            if (!isBlocked.get()) {
                inFirstState = if (inFirstState) {
                    firstAction()
                    activityOfButton.runOnUiThread { button.text = secondTextValue }
                    false
                } else {
                    secondAction()
                    activityOfButton.runOnUiThread { button.text = firstTextValue }
                    true
                }
            }
        }
    }

    fun lock() {
        isBlocked.set(true)
    }

    fun unlock() {
        isBlocked.set(false)
    }
}
