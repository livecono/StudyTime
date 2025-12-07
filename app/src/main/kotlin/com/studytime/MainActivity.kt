package com.studytime

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {
    private lateinit var timerDisplay: TextView
    private lateinit var emptyTimerText: TextView
    private lateinit var btnSettings: Button
    private lateinit var customTimerContainer: GridLayout
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var messageRepository: MessageRepository
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var selectedMinutes: Int = 0
    private var isTimerRunning: Boolean = false
    private var isPaused: Boolean = false
    private var remainingMillis: Long = 0L
    private var wasTimerRunningBeforePause: Boolean = false
    private lateinit var timerBroadcastReceiver: TimerBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        initializeViews()
        initializeManagers()
        applyUIColors()
        setupClickListeners()
        updateCustomTimerButtons()
    }

    private fun initializeViews() {
        timerDisplay = findViewById(R.id.timerDisplay)
        emptyTimerText = findViewById(R.id.emptyTimerText)
        btnSettings = findViewById(R.id.btnSettings)
        customTimerContainer = findViewById(R.id.customTimerContainer)
    }

    private fun initializeManagers() {
        preferenceManager = PreferenceManager(this)
        messageRepository = MessageRepository(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // TimerService로부터 브로드캐스트 수신
        timerBroadcastReceiver = TimerBroadcastReceiver().apply {
            onUpdate = { millisUntilFinished ->
                remainingMillis = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)
            }
            onFinish = {
                handleTimerFinish()
            }
        }

        val filter = IntentFilter("com.studytime.TIMER_UPDATE")
        filter.addAction("com.studytime.TIMER_FINISHED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(timerBroadcastReceiver, filter)
        }
    }

    private fun checkDeviceAdminPermission() {
        if (preferenceManager.isFirstRun()) {
            preferenceManager.setFirstRunDone()

            if (!isDeviceAdminActive()) {
                showDeviceAdminDialog()
            }
        }
    }

    private fun isDeviceAdminActive(): Boolean {
        // Device Admin 권한 제거됨
        return true
    }

    private fun showDeviceAdminDialog() {
        // Device Admin 권한 제거됨
    }

    private fun enableDeviceAdmin() {
        // Device Admin 권한 제거됨
    }



    private fun applyUIColors() {
        val colorName = preferenceManager.getUIColor()
        val primaryColor = ColorUtils.getPrimaryColor(this, colorName)
        timerDisplay.setTextColor(primaryColor)
        emptyTimerText.setTextColor(primaryColor)
        btnSettings.setBackgroundColor(primaryColor)
    }

    private fun setupClickListeners() {
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun selectTimer(minutes: Int = 0, seconds: Int = 0) {
        if (!isTimerRunning) {
            val totalSeconds = if (seconds > 0) seconds else minutes * 60
            selectedMinutes = if (seconds > 0) 0 else minutes
            remainingMillis = (totalSeconds * 1000).toLong()
            updateTimerDisplay(remainingMillis)
            startTimer()
        }
    }

    private fun updateCustomTimerButtons() {
        customTimerContainer.removeAllViews()
        val customTimers = preferenceManager.getCustomTimers().sorted()

        if (customTimers.isEmpty()) {
            emptyTimerText.visibility = android.view.View.VISIBLE
            return
        } else {
            emptyTimerText.visibility = android.view.View.GONE
        }

        // 타이머 개수가 1개 또는 2개면 weight 미사용, 3개 이상이면 weight 사용
        val useWeight = customTimers.size >= 3
        val weight = if (useWeight) 1f else 0f

        for (minutes in customTimers) {
            val btn = Button(this).apply {
                text = "${minutes}분"
                layoutParams = GridLayout.LayoutParams().apply {
                    width = if (useWeight) 0 else GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, weight)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener { 
                    selectTimer(minutes)
                }
                setOnLongClickListener {
                    preferenceManager.removeCustomTimer(minutes)
                    updateCustomTimerButtons()
                    true
                }
            }
            customTimerContainer.addView(btn)
        }
    }

    private fun startTimer() {
        if (remainingMillis == 0L) {
            Toast.makeText(this, "타이머를 선택하세요", Toast.LENGTH_SHORT).show()
            return
        }

        isTimerRunning = true
        isPaused = false
        
        // 이전 타이머 상태 초기화
        preferenceManager.clearTimerState()
        
        val lockScreenIntent = Intent(this, LockScreenActivity::class.java)
        lockScreenIntent.putExtra("initialDuration", remainingMillis)
        startActivity(lockScreenIntent)
        
        val serviceIntent = Intent(this, TimerService::class.java)
        serviceIntent.action = if (isPaused) "RESUME_TIMER" else "START_TIMER"
        serviceIntent.putExtra("duration", remainingMillis)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        acquireWakeLock()
        setKeepScreenOn(true)
    }

    private fun handleTimerFinish() {
        isTimerRunning = false
        selectedMinutes = 0
        releaseWakeLock()
        setKeepScreenOn(false)

        updateTimerDisplay(0)

        val praise = messageRepository.getRandomMessage()
        showPraiseDialog(praise)
        
        playCompletionNotification()
        showCompletionNotification(praise)
    }

    private fun showPraiseDialog(praise: String) {
        val userName = preferenceManager.getUserName()
        val message = if (userName.isNotEmpty()) {
            val particle = if (userName.endsWith("이")) "여" else if (isConsonantEnding(userName)) "아" else "야"
            "$userName$particle, $praise"
        } else {
            praise
        }
        
        AlertDialog.Builder(this)
            .setTitle("Completed!")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
            }
            .setCancelable(false)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel",
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for timer completion"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showCompletionNotification(praise: String) {
        val userName = preferenceManager.getUserName()
        val notificationTitle = if (userName.isNotEmpty()) {
            val particle = if (userName.endsWith("이")) "여" else if (isConsonantEnding(userName)) "아" else "야"
            "$userName$particle, 타이머 완료!"
        } else {
            "타이머 완료!"
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle(notificationTitle)
            .setContentText(praise)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification)
    }

    private fun isConsonantEnding(name: String): Boolean {
        if (name.isEmpty()) return false
        val lastChar = name.last()
        val unicode = lastChar.code
        // 한글 자모가 있는지 확인 (초성+중성+종성)
        return (unicode - 0xAC00) % 28 != 0
    }

    private fun updateTimerDisplay(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 1000 / 60
        val seconds = (millisUntilFinished / 1000) % 60
        timerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "StudyTime::TimerWakeLock"
            )
            wakeLock?.acquire(24 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
            wakeLock = null
        }
    }

    private fun setKeepScreenOn(keep: Boolean) {
        if (keep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        // 타이머가 실행 중이면 상태 저장 (TimerManager는 시스템 시간 기반이라 계속 실행됨)
        wasTimerRunningBeforePause = isTimerRunning
    }

    override fun onResume() {
        super.onResume()
        applyUIColors()
        updateCustomTimerButtons()
        
        // 타이머 상태 확인 (LockScreenActivity에서 돌아왔을 수 있음)
        if (!preferenceManager.isTimerRunning()) {
            val endTime = preferenceManager.getTimerEndTime()
            if (endTime == 0L) {
                // 타이머 완전 종료 상태
                timerDisplay.text = "00:00"
                isTimerRunning = false
                selectedMinutes = 0
                remainingMillis = 0L
            }
        }
    }

    private fun playCompletionNotification() {
        val notificationType = preferenceManager.getNotificationType()
        val durationSeconds = preferenceManager.getNotificationDuration()

        when (notificationType) {
            "vibration" -> vibrate(durationSeconds)
            "sound" -> playSound(durationSeconds)
            "both" -> {
                playSound(durationSeconds)
                vibrate(durationSeconds)
            }
        }
    }

    private fun vibrate(durationSeconds: Int) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val totalDurationMillis = (durationSeconds * 1000).toLong()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 500ms 진동, 200ms 휴식을 반복 (총 700ms가 한 사이클)
                val pattern = longArrayOf(0, 500, 200)
                // repeatIndex를 1로 설정하면 pattern[1]부터 계속 반복
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 1))
                
                // 설정 시간 후 진동 중지
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        vibrator.cancel()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to cancel vibration: ${e.message}")
                    }
                }, totalDurationMillis)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 500, 200)
                vibrator.vibrate(pattern, 1)
                
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        vibrator.cancel()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to cancel vibration: ${e.message}")
                    }
                }, totalDurationMillis)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to vibrate: ${e.message}")
        }
    }

    private fun playSound(durationSeconds: Int) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, notificationUri)
            
            val startTime = System.currentTimeMillis()
            val durationMillis = (durationSeconds * 1000).toLong()
            
            ringtone.play()
            
            // 설정된 시간 동안 계속 소리 유지 (중단되면 재시작)
            val checkRunnable = object : Runnable {
                override fun run() {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    
                    if (elapsedTime < durationMillis) {
                        try {
                            if (!ringtone.isPlaying) {
                                ringtone.play()
                            }
                            Handler(Looper.getMainLooper()).postDelayed(this, 100)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error during sound playback: ${e.message}")
                        }
                    } else {
                        try {
                            if (ringtone.isPlaying) {
                                ringtone.stop()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to stop sound: ${e.message}")
                        }
                    }
                }
            }
            Handler(Looper.getMainLooper()).post(checkRunnable)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to play sound: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        try {
            unregisterReceiver(timerBroadcastReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
