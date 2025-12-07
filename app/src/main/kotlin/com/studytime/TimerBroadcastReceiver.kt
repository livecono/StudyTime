package com.studytime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerBroadcastReceiver : BroadcastReceiver() {
    var onUpdate: ((Long) -> Unit)? = null
    var onFinish: (() -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.studytime.TIMER_UPDATE" -> {
                val millisUntilFinished = intent.getLongExtra("millisUntilFinished", 0L)
                onUpdate?.invoke(millisUntilFinished)
            }
            "com.studytime.TIMER_FINISHED" -> {
                onFinish?.invoke()
            }
        }
    }
}
