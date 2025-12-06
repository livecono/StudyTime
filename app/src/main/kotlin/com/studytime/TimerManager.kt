package com.studytime

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class TimerManager(private val context: Context, private val onTickCallback: (Long) -> Unit, private val onFinishCallback: () -> Unit) {
    private var countDownTimer: CountDownTimer? = null
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun startTimer(durationMinutes: Int) {
        val durationMillis = durationMinutes * 60 * 1000L
        startTimerMillis(durationMillis)
    }

    fun startTimerMillis(durationMillis: Long) {
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTickCallback(millisUntilFinished)
                // 타이머 실행 중 매번 잠금화면 유지
                maintainFocusMode()
            }

            override fun onFinish() {
                lockScreen()
                playCompletionSound()
                vibrateOnCompletion()
                // 타이머 완료 브로드캐스트 전송
                sendTimerFinishBroadcast()
                onFinishCallback()
            }
        }

        countDownTimer?.start()
        // 시작 시 포커스 모드 활성화
        maintainFocusMode()
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
    }

    fun resumeTimer(remainingMillis: Long) {
        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                onTickCallback(millisUntilFinished)
                maintainFocusMode()
            }

            override fun onFinish() {
                lockScreen()
                playCompletionSound()
                vibrateOnCompletion()
                // 타이머 완료 브로드캐스트 전송
                sendTimerFinishBroadcast()
                onFinishCallback()
            }
        }

        countDownTimer?.start()
        maintainFocusMode()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        unlockScreen()
    }

    private fun maintainFocusMode() {
        try {
            val admins = devicePolicyManager.activeAdmins
            if (admins != null && admins.isNotEmpty()) {
                // 카메라 비활성화로 앱 접근 제한
                devicePolicyManager.setCameraDisabled(admins[0], true)
            }
        } catch (e: Exception) {
            Log.e("TimerManager", "Failed to maintain focus mode: ${e.message}")
        }
    }

    private fun lockScreen() {
        try {
            val admins = devicePolicyManager.activeAdmins
            if (admins != null && admins.isNotEmpty()) {
                // 다른 앱 실행 방지
                devicePolicyManager.setCameraDisabled(admins[0], true)
                // 잠금화면 표시
                devicePolicyManager.lockNow()
                Log.i("TimerManager", "Screen locked successfully")
            }
        } catch (e: Exception) {
            Log.e("TimerManager", "Failed to lock screen: ${e.message}")
        }
    }

    private fun unlockScreen() {
        try {
            val admins = devicePolicyManager.activeAdmins
            if (admins != null && admins.isNotEmpty()) {
                devicePolicyManager.setCameraDisabled(admins[0], false)
            }
        } catch (e: Exception) {
            Log.e("TimerManager", "Failed to unlock: ${e.message}")
        }
    }

    private fun playCompletionSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("TimerManager", "Failed to play sound: ${e.message}")
        }
    }

    private fun vibrateOnCompletion() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            Log.e("TimerManager", "Failed to vibrate: ${e.message}")
        }
    }

    private fun sendTimerFinishBroadcast() {
        try {
            val intent = Intent("com.studytime.TIMER_FINISHED")
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            Log.i("TimerManager", "Timer finish broadcast sent")
        } catch (e: Exception) {
            Log.e("TimerManager", "Failed to send timer finish broadcast: ${e.message}")
        }
    }
}
