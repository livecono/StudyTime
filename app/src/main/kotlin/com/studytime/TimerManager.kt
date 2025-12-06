package com.studytime

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.concurrent.thread

class TimerManager(private val context: Context, private val onTickCallback: (Long) -> Unit, private val onFinishCallback: () -> Unit) {
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    
    // 백그라운드 스레드 기반 타이머
    private var timerThread: Thread? = null
    private var isTimerRunning = false
    private var timerEndTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startTimer(durationMinutes: Int) {
        val durationMillis = durationMinutes * 60 * 1000L
        startTimerMillis(durationMillis)
    }

    fun startTimerMillis(durationMillis: Long) {
        // 기존 타이머 취소
        cancelTimer()
        
        // 시스템 시간 기반 타이머 시작
        isTimerRunning = true
        timerEndTime = System.currentTimeMillis() + durationMillis
        Log.i("TimerManager", "Timer started: ${durationMillis}ms, endTime: $timerEndTime")
        
        maintainFocusMode()
        
        // 백그라운드 스레드에서 타이머 실행
        timerThread = thread(isDaemon = false) {
            try {
                while (isTimerRunning && System.currentTimeMillis() < timerEndTime) {
                    val currentTime = System.currentTimeMillis()
                    val remainingMillis = timerEndTime - currentTime
                    
                    if (remainingMillis <= 0) {
                        break
                    }
                    
                    // UI 스레드에서 콜백 실행
                    mainHandler.post {
                        if (isTimerRunning) {
                            onTickCallback(remainingMillis)
                        }
                    }
                    
                    // 100ms 대기
                    Thread.sleep(100)
                }
                
                // 타이머 완료
                if (isTimerRunning) {
                    isTimerRunning = false
                    mainHandler.post {
                        lockScreen()
                        playCompletionSound()
                        vibrateOnCompletion()
                        sendTimerFinishBroadcast()
                        onFinishCallback()
                    }
                }
            } catch (e: InterruptedException) {
                Log.d("TimerManager", "Timer thread interrupted")
            } catch (e: Exception) {
                Log.e("TimerManager", "Timer thread error: ${e.message}")
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerThread?.interrupt()
        timerThread = null
        Log.i("TimerManager", "Timer paused")
    }

    fun resumeTimer(remainingMillis: Long) {
        // 기존 타이머 취소
        pauseTimer()
        
        // 새로운 종료 시간 계산
        isTimerRunning = true
        timerEndTime = System.currentTimeMillis() + remainingMillis
        Log.i("TimerManager", "Timer resumed: ${remainingMillis}ms, endTime: $timerEndTime")
        
        maintainFocusMode()
        
        // 백그라운드 스레드에서 타이머 실행
        timerThread = thread(isDaemon = false) {
            try {
                while (isTimerRunning && System.currentTimeMillis() < timerEndTime) {
                    val currentTime = System.currentTimeMillis()
                    val remainingMillis = timerEndTime - currentTime
                    
                    if (remainingMillis <= 0) {
                        break
                    }
                    
                    // UI 스레드에서 콜백 실행
                    mainHandler.post {
                        if (isTimerRunning) {
                            onTickCallback(remainingMillis)
                        }
                    }
                    
                    // 100ms 대기
                    Thread.sleep(100)
                }
                
                // 타이머 완료
                if (isTimerRunning) {
                    isTimerRunning = false
                    mainHandler.post {
                        lockScreen()
                        playCompletionSound()
                        vibrateOnCompletion()
                        sendTimerFinishBroadcast()
                        onFinishCallback()
                    }
                }
            } catch (e: InterruptedException) {
                Log.d("TimerManager", "Timer thread interrupted")
            } catch (e: Exception) {
                Log.e("TimerManager", "Timer thread error: ${e.message}")
            }
        }
    }

    fun cancelTimer() {
        isTimerRunning = false
        timerThread?.interrupt()
        timerThread = null
        unlockScreen()
        Log.i("TimerManager", "Timer cancelled")
    }

    private fun maintainFocusMode() {
        try {
            val admins = devicePolicyManager.activeAdmins
            if (admins != null && admins.isNotEmpty()) {
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
                devicePolicyManager.setCameraDisabled(admins[0], true)
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
