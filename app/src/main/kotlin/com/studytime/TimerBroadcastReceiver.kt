package com.studytime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerBroadcastReceiver(
    private val onUpdate: (Long) -> Unit,
    private val onFinish: (() -> Unit)? = null
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.studytime.TIMER_UPDATE" -> {
                val millisUntilFinished = intent.getLongExtra("millisUntilFinished", 0L)
                onUpdate(millisUntilFinished)
            }
            "com.studytime.TIMER_FINISHED" -> {
                onFinish?.invoke()
            }
        }
    }
}
