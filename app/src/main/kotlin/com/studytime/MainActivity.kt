package com.studytime

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var timerDisplay: TextView
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnSettings: Button
    private lateinit var customTimerInput: EditText
    private lateinit var btnCustomTimer: Button
    private lateinit var customTimerContainer: LinearLayout
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var messageRepository: MessageRepository
    private lateinit var timerManager: TimerManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var selectedMinutes: Int = 0
    private var isTimerRunning: Boolean = false
    private var isPaused: Boolean = false
    private var remainingMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeManagers()
        checkDeviceAdminPermission()
        setupClickListeners()
        updateCustomTimerButtons()
    }

    private fun initializeViews() {
        timerDisplay = findViewById(R.id.timerDisplay)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        btnSettings = findViewById(R.id.btnSettings)
        customTimerInput = findViewById(R.id.customTimerInput)
        btnCustomTimer = findViewById(R.id.btnCustomTimer)
        customTimerContainer = findViewById(R.id.customTimerContainer)
    }

    private fun initializeManagers() {
        preferenceManager = PreferenceManager(this)
        messageRepository = MessageRepository(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        timerManager = TimerManager(
            context = this,
            onTickCallback = { millisUntilFinished ->
                remainingMillis = millisUntilFinished
                updateTimerDisplay(millisUntilFinished)
                broadcastTimerUpdate(millisUntilFinished)
            },
            onFinishCallback = {
                handleTimerFinish()
            }
        )
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
        val componentName = ComponentName(this, AdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(componentName)
    }

    private fun showDeviceAdminDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.device_admin_required)
            .setMessage(R.string.device_admin_required_message)
            .setPositiveButton(R.string.enable_device_admin) { _, _ ->
                enableDeviceAdmin()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                Toast.makeText(this, "Cannot use focus mode without Device Admin", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableDeviceAdmin() {
        val componentName = ComponentName(this, AdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device Admin permission required for focus mode.")
        }
        startActivity(intent)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btn5sec).setOnClickListener { selectTimer(seconds = 5) }
        findViewById<Button>(R.id.btn5min).setOnClickListener { selectTimer(minutes = 5) }
        findViewById<Button>(R.id.btn10min).setOnClickListener { selectTimer(minutes = 10) }
        findViewById<Button>(R.id.btn15min).setOnClickListener { selectTimer(minutes = 15) }
        findViewById<Button>(R.id.btn25min).setOnClickListener { selectTimer(minutes = 25) }
        findViewById<Button>(R.id.btn50min).setOnClickListener { selectTimer(minutes = 50) }

        btnCustomTimer.setOnClickListener { addCustomTimer() }

        btnStart.setOnClickListener { startTimer() }
        btnPause.setOnClickListener { pauseTimer() }
        btnStop.setOnClickListener { stopTimer() }

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
            val displayText = if (seconds > 0) "$seconds second timer started" else "$minutes minute timer started"
            Toast.makeText(this, displayText, Toast.LENGTH_SHORT).show()
            startTimer()
        }
    }

    private fun addCustomTimer() {
        val input = customTimerInput.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter minutes", Toast.LENGTH_SHORT).show()
            return
        }

        val minutes = input.toIntOrNull() ?: return
        if (minutes <= 0 || minutes > 1440) {
            Toast.makeText(this, "Enter value between 1-1440", Toast.LENGTH_SHORT).show()
            return
        }

        preferenceManager.addCustomTimer(minutes)
        customTimerInput.text.clear()
        updateCustomTimerButtons()
        Toast.makeText(this, "$minutes minute timer added", Toast.LENGTH_SHORT).show()
    }

    private fun updateCustomTimerButtons() {
        customTimerContainer.removeAllViews()
        val customTimers = preferenceManager.getCustomTimers()

        for (minutes in customTimers) {
            val btn = Button(this).apply {
                text = "$minutes min"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener { selectTimer(minutes) }
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
            Toast.makeText(this, "Select a timer", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isDeviceAdminActive()) {
            Toast.makeText(this, "Device Admin permission required", Toast.LENGTH_SHORT).show()
            return
        }

        if (isPaused) {
            timerManager.resumeTimer(remainingMillis)
            isPaused = false
        } else {
            isTimerRunning = true
            timerManager.startTimerMillis(remainingMillis)
            acquireWakeLock()
            setKeepScreenOn(true)
            // 잠금화면 액티비티 표시
            startActivity(Intent(this, LockScreenActivity::class.java))
        }

        isTimerRunning = true
        btnStart.isEnabled = false
        btnPause.isEnabled = true
        btnStop.isEnabled = true
    }

    private fun pauseTimer() {
        if (isTimerRunning) {
            timerManager.pauseTimer()
            isPaused = true
            btnStart.isEnabled = true
            btnStart.text = "Resume"
            btnPause.isEnabled = false
            btnStop.isEnabled = true
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        isPaused = false
        selectedMinutes = 0
        remainingMillis = 0
        timerManager.cancelTimer()
        updateTimerDisplay(0)
        releaseWakeLock()
        setKeepScreenOn(false)

        btnStart.isEnabled = true
        btnStart.text = "Start"
        btnPause.isEnabled = false
        btnStop.isEnabled = false
    }

    private fun handleTimerFinish() {
        isTimerRunning = false
        selectedMinutes = 0
        releaseWakeLock()
        setKeepScreenOn(false)

        btnStart.isEnabled = true
        btnStart.text = "Start"
        btnPause.isEnabled = false
        btnStop.isEnabled = false
        
        // 타이머 디스플레이를 00:00으로 초기화
        updateTimerDisplay(0)

        val praise = messageRepository.getRandomMessage()
        showPraiseDialog(praise)
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
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
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

    private fun broadcastTimerUpdate(millisUntilFinished: Long) {
        val intent = Intent("com.studytime.TIMER_UPDATE").apply {
            putExtra("millisUntilFinished", millisUntilFinished)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        } else {
            sendBroadcast(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        timerManager.cancelTimer()
    }
}
