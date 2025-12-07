package com.studytime

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LockScreenActivity : AppCompatActivity() {
    private lateinit var timerDisplay: TextView
    private lateinit var studyingStatusText: TextView
    private lateinit var btnPauseScreen: Button
    private lateinit var btnStopScreen: Button
    private lateinit var timerBroadcastReceiver: TimerBroadcastReceiver
    private var isReceiverRegistered = false
    private lateinit var preferenceManager: PreferenceManager
    private val updateHandler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private var isPaused = false
    private var remainingMillisOnPause = 0L

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

        preferenceManager = PreferenceManager(this)
        timerDisplay = findViewById(R.id.lockScreenTimerDisplay)
        studyingStatusText = findViewById(R.id.studyingStatusText)
        btnPauseScreen = findViewById(R.id.btnPauseScreen)
        btnStopScreen = findViewById(R.id.btnStopScreen)
        
        // 사용자 이름 설정
        val userName = preferenceManager.getUserName()
        studyingStatusText.text = if (userName.isNotEmpty()) {
            "$userName 공부중..."
        } else {
            "공부중..."
        }
        
        // 버튼 리스너 설정
        btnPauseScreen.setOnClickListener { pauseTimer() }
        btnStopScreen.setOnClickListener { stopTimer() }
        
        // 초기 타이머 값 설정
        val initialDuration = intent.getLongExtra("initialDuration", 0L)
        if (initialDuration > 0) {
            updateTimerDisplay(initialDuration)
        } else {
            timerDisplay.text = "00:00"
        }

        // 브로드캐스트 리시버 생성
        timerBroadcastReceiver = TimerBroadcastReceiver().apply {
            onUpdate = { millisUntilFinished ->
                updateTimerDisplay(millisUntilFinished)
            }
            onFinish = {
                // 타이머 완료 - 자동으로 홈화면 복귀
                timerDisplay.text = "00:00"
                preferenceManager.clearTimerState()
                finish()
            }
        }

        // 주기적으로 SharedPreferences에서 타이머 상태 읽기
        updateRunnable = object : Runnable {
            override fun run() {
                val endTime = preferenceManager.getTimerEndTime()
                val remainingMillis = preferenceManager.getTimerRemainingMillis()
                val isRunning = preferenceManager.isTimerRunning()
                val currentTime = System.currentTimeMillis()
                
                android.util.Log.d("LockScreenActivity", "Update: endTime=$endTime, remainingMillis=$remainingMillis, isRunning=$isRunning")
                
                if (isRunning && endTime > 0 && endTime > currentTime) {
                    // 타이머 실행 중
                    val timeRemaining = endTime - currentTime
                    updateTimerDisplay(timeRemaining)
                    updateHandler.postDelayed(this, 100)
                } else if (!isRunning && remainingMillis > 0) {
                    // 일시정지 상태
                    updateTimerDisplay(remainingMillis)
                    updateHandler.postDelayed(this, 500)
                } else if (endTime > 0 && endTime <= currentTime) {
                    // 타이머 완료
                    timerDisplay.text = "00:00"
                    preferenceManager.clearTimerState()
                    finish()
                } else {
                    // 아직 타이머 시작 안 됨
                    val initialDuration = intent.getLongExtra("initialDuration", 0L)
                    if (initialDuration > 0) {
                        updateTimerDisplay(initialDuration)
                    }
                    updateHandler.postDelayed(this, 500)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 타이머 업데이트 시작
        updateHandler.post(updateRunnable)
        
        // onResume에서 브로드캐스트 등록 - 화면이 보일 때 반드시 등록
        if (!isReceiverRegistered) {
            val filter = IntentFilter("com.studytime.TIMER_UPDATE")
            filter.addAction("com.studytime.TIMER_FINISHED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(timerBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(timerBroadcastReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        // 타이머 업데이트 중단
        updateHandler.removeCallbacks(updateRunnable)
        
        // onPause에서 브로드캐스트 등록 해제
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(timerBroadcastReceiver)
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
        // 터치 입력은 일반적인 dispatchTouchEvent로 처리되므로 super 호출
        return super.onTouchEvent(event)
    }

    private fun pauseTimer() {
        android.util.Log.d("LockScreenActivity", "pauseTimer clicked, isPaused=$isPaused")
        if (isPaused) {
            // 다시 시작
            android.util.Log.d("LockScreenActivity", "Resuming timer")
            val serviceIntent = Intent(this, TimerService::class.java)
            serviceIntent.action = "RESUME_TIMER"
            startService(serviceIntent)
            btnPauseScreen.text = "일시정지"
            isPaused = false
        } else {
            // 일시정지
            android.util.Log.d("LockScreenActivity", "Pausing timer")
            val serviceIntent = Intent(this, TimerService::class.java)
            serviceIntent.action = "PAUSE_TIMER"
            startService(serviceIntent)
            btnPauseScreen.text = "다시시작"
            isPaused = true
        }
    }

    private fun stopTimer() {
        val serviceIntent = Intent(this, TimerService::class.java)
        serviceIntent.action = "STOP_TIMER"
        startService(serviceIntent)
        
        // 타이머 상태 완전 초기화
        preferenceManager.clearTimerState()
        timerDisplay.text = "00:00"
        
        // 100ms 대기 후 종료 (SharedPreferences 저장 보장)
        updateHandler.postDelayed({
            finish()
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.removeCallbacks(updateRunnable)
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(timerBroadcastReceiver)
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
