package com.studytime

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var customTimersListContainer: LinearLayout
    private lateinit var btnBack: Button
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var userNameInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        customTimersListContainer = findViewById(R.id.customTimersListContainer)
        btnBack = findViewById(R.id.btnBack)
        userNameInput = findViewById(R.id.userNameInput)
        preferenceManager = PreferenceManager(this)

        // 저장된 사용자 이름 로드
        userNameInput.setText(preferenceManager.getUserName())

        btnBack.setOnClickListener { 
            // 사용자 이름 저장
            val userName = userNameInput.text.toString().trim()
            preferenceManager.setUserName(userName)
            finish() 
        }

        updateCustomTimersList()
    }

    private fun updateCustomTimersList() {
        customTimersListContainer.removeAllViews()
        val customTimers = preferenceManager.getCustomTimers()

        if (customTimers.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "추가된 커스텀 타이머가 없습니다."
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
                textSize = 14f
            }
            customTimersListContainer.addView(emptyText)
        } else {
            for (minutes in customTimers) {
                val itemLayout = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                    setPadding(16, 16, 16, 16)
                }

                val timerText = TextView(this).apply {
                    text = "${minutes}분"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    textSize = 16f
                }

                val deleteBtn = Button(this).apply {
                    text = "삭제"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 1f
                        marginStart = 16
                    }
                    setOnClickListener {
                        preferenceManager.removeCustomTimer(minutes)
                        updateCustomTimersList()
                        Toast.makeText(this@SettingsActivity, "${minutes}분 타이머 삭제됨", Toast.LENGTH_SHORT).show()
                    }
                }

                itemLayout.addView(timerText)
                itemLayout.addView(deleteBtn)
                customTimersListContainer.addView(itemLayout)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateCustomTimersList()
    }
}
