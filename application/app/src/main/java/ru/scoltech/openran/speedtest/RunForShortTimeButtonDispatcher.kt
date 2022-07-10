package ru.scoltech.openran.speedtest

import android.app.Activity
import android.widget.Button
import java.util.concurrent.atomic.AtomicBoolean

class RunForShortTimeButtonDispatcher(
    button: Button,
    activityOfButton: Activity,
    activeTextValue: String = button.text.toString(),
    action: (() -> Unit) -> Unit
) {
    var inAction = AtomicBoolean(false)
    private val defaultTextValue = button.text.toString()
    init {
        button.setOnClickListener {
            if (!inAction.get()) {
                inAction.set(true)
                activityOfButton.runOnUiThread{
                    button.text = activeTextValue
                }
                action {
                    inAction.set(false)
                    activityOfButton.runOnUiThread{
                        button.text = defaultTextValue
                    }
                }
            }
        }
    }

}
