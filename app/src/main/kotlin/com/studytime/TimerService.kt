package com.studytime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class TimerService : Service() {
    private var timerThread: Thread? = null
    private var isTimerRunning = false
    private var timerEndTime: Long = 0L
    private lateinit var preferenceManager: PreferenceManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        preferenceManager = PreferenceManager(this)
        Log.i("TimerService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val durationMillis = intent?.getLongExtra("duration", 0L) ?: 0L

        Log.i("TimerService", "onStartCommand: action=$action, duration=$durationMillis")

        when (action) {
            "START_TIMER" -> {
                Log.i("TimerService", "Starting timer: ${durationMillis}ms")
                startTimer(durationMillis)
            }
            "STOP_TIMER" -> {
                Log.i("TimerService", "Stopping timer")
                stopTimer()
                stopSelf()
            }
            "PAUSE_TIMER" -> {
                Log.i("TimerService", "Pausing timer")
                pauseTimer()
            }
            "RESUME_TIMER" -> {
                Log.i("TimerService", "Resuming timer")
                // 일시정지된 남은 시간을 읽어서 새로운 endTime 설정
                val remainingMillis = preferenceManager.getTimerRemainingMillis()
                if (remainingMillis > 0) {
                    startTimer(remainingMillis)
                } else {
                    // 저장된 endTime이 있으면 그것도 시도
                    val savedEndTime = preferenceManager.getTimerEndTime()
                    val timeRemaining = savedEndTime - System.currentTimeMillis()
                    if (timeRemaining > 0) {
                        startTimer(timeRemaining)
                    } else {
                        Log.w("TimerService", "No valid timer to resume")
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        // 기존 타이머 취소
        stopTimer()

        isTimerRunning = true
        timerEndTime = System.currentTimeMillis() + durationMillis
        Log.i("TimerService", "Timer started: ${durationMillis}ms, endTime: $timerEndTime")

        // SharedPreferences에 타이머 상태 저장
        try {
            preferenceManager.saveTimerState(timerEndTime, true)
            Log.i("TimerService", "Timer state saved to SharedPreferences: endTime=$timerEndTime")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to save timer state: ${e.message}", e)
        }

        // 포그라운드 서비스 알림 표시
        updateNotification(durationMillis)
        
        // 초기 브로드캐스트 - 화면에 즉시 표시
        broadcastTimerUpdate(durationMillis)

        // 백그라운드 스레드에서 타이머 실행
        timerThread = thread(isDaemon = false) {
            try {
                while (isTimerRunning && System.currentTimeMillis() < timerEndTime) {
                    val currentTime = System.currentTimeMillis()
                    val remainingMillis = timerEndTime - currentTime

                    if (remainingMillis <= 0) {
                        break
                    }

                    // SharedPreferences 업데이트
                    try {
                        preferenceManager.saveTimerState(timerEndTime, true)
                    } catch (e: Exception) {
                        Log.e("TimerService", "Failed to update timer state: ${e.message}")
                    }

                    // 타이머 업데이트 브로드캐스트
                    broadcastTimerUpdate(remainingMillis)
                    updateNotification(remainingMillis)

                    // 100ms 대기
                    Thread.sleep(100)
                }

                // 타이머 완료
                if (isTimerRunning) {
                    isTimerRunning = false
                    preferenceManager.clearTimerState()
                    broadcastTimerFinished()
                    playCompletionSound()
                    vibrateOnCompletion()
                    Log.i("TimerService", "Timer finished")
                    stopSelf()
                }
            } catch (e: InterruptedException) {
                Log.d("TimerService", "Timer thread interrupted")
            } catch (e: Exception) {
                Log.e("TimerService", "Timer thread error: ${e.message}", e)
            }
        }
    }

    private fun pauseTimer() {
        Log.i("TimerService", "pauseTimer called")
        if (!isTimerRunning) {
            Log.d("TimerService", "Timer already paused, ignoring pause request")
            return
        }
        
        isTimerRunning = false
        // 현재 남은 시간을 계산해서 저장
        val currentTime = System.currentTimeMillis()
        val remainingMillis = timerEndTime - currentTime
        if (remainingMillis > 0) {
            // SharedPreferences에만 남은 시간 저장, 다시 시작할 때 사용
            preferenceManager.saveTimerPausedState(remainingMillis)
            Log.i("TimerService", "Timer paused: remainingMillis=$remainingMillis")
        }
        timerThread?.interrupt()
        timerThread = null
    }

    private fun stopTimer() {
        isTimerRunning = false
        // 타이머 상태 완전히 초기화
        preferenceManager.clearTimerState()
        timerThread?.interrupt()
        timerThread = null
    }

    private fun broadcastTimerUpdate(remainingMillis: Long) {
        try {
            val intent = Intent("com.studytime.TIMER_UPDATE")
            intent.putExtra("millisUntilFinished", remainingMillis)
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to send timer update: ${e.message}")
        }
    }

    private fun broadcastTimerFinished() {
        try {
            val intent = Intent("com.studytime.TIMER_FINISHED")
            sendBroadcast(intent)
            Log.i("TimerService", "Timer finish broadcast sent")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to send timer finish broadcast: ${e.message}")
        }
    }

    private fun updateNotification(remainingMillis: Long) {
        val minutes = remainingMillis / 1000 / 60
        val seconds = (remainingMillis / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        // MainActivity로 돌아가는 Intent
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        mainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Timer: $timeText")
            .setContentText("공부 중...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.setShowBadge(true)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playCompletionSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to play sound: ${e.message}")
        }
    }

    private fun vibrateOnCompletion() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to vibrate: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        Log.i("TimerService", "Service destroyed")
    }
}
