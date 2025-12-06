package com.studytime

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class LockScreenActivity : AppCompatActivity() {
    private lateinit var timerDisplay: TextView
    private lateinit var timerBroadcastReceiver: TimerBroadcastReceiver
    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)

        // 잠금화면 설정
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        timerDisplay = findViewById(R.id.lockScreenTimerDisplay)
        timerDisplay.text = "00:00"

        // 브로드캐스트 리시버 생성
        timerBroadcastReceiver = TimerBroadcastReceiver(
            onUpdate = { millisUntilFinished ->
                updateTimerDisplay(millisUntilFinished)
            },
            onFinish = {
                // 타이머 완료 - 자동으로 홈화면 복귀
                timerDisplay.text = "00:00"
                finish()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // onResume에서 브로드캐스트 등록 - 화면이 보일 때 반드시 등록
        if (!isReceiverRegistered) {
            val filter = IntentFilter("com.studytime.TIMER_UPDATE")
            filter.addAction("com.studytime.TIMER_FINISHED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalBroadcastManager.getInstance(this).registerReceiver(timerBroadcastReceiver, filter)
            } else {
                registerReceiver(timerBroadcastReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        // onPause에서 브로드캐스트 등록 해제
        if (isReceiverRegistered) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(timerBroadcastReceiver)
                } else {
                    unregisterReceiver(timerBroadcastReceiver)
                }
            } catch (e: Exception) {
                // ignore
            }
            isReceiverRegistered = false
        }
    }

    private fun updateTimerDisplay(millisUntilFinished: Long) {
        runOnUiThread {
            val minutes = millisUntilFinished / 1000 / 60
            val seconds = (millisUntilFinished / 1000) % 60
            timerDisplay.text = String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 터치 입력 무시
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(timerBroadcastReceiver)
                } else {
                    unregisterReceiver(timerBroadcastReceiver)
                }
            } catch (e: Exception) {
                // ignore
            }
            isReceiverRegistered = false
        }
    }

    override fun onBackPressed() {
        // 백 버튼 비활성화
    }
}
