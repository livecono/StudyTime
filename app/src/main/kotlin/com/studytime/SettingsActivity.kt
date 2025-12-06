package com.studytime

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {
    private lateinit var customTimersListContainer: LinearLayout
    private lateinit var colorSelectionContainer: LinearLayout
    private lateinit var timerInputField: EditText
    private lateinit var btnAddTimer: Button
    private lateinit var btnBack: Button
    private lateinit var userNameInput: EditText
    private lateinit var preferenceManager: PreferenceManager

    private val colors = mapOf(
        "purple" to R.color.purple_500,
        "blue" to R.color.blue_500,
        "red" to R.color.red_500,
        "green" to R.color.green_500,
        "orange" to R.color.orange_500,
        "pink" to R.color.pink_500,
        "teal" to R.color.teal_700
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        customTimersListContainer = findViewById(R.id.customTimersListContainer)
        colorSelectionContainer = findViewById(R.id.colorSelectionContainer)
        timerInputField = findViewById(R.id.timerInputField)
        btnAddTimer = findViewById(R.id.btnAddTimer)
        btnBack = findViewById(R.id.btnBack)
        userNameInput = findViewById(R.id.userNameInput)
        preferenceManager = PreferenceManager(this)

        userNameInput.setText(preferenceManager.getUserName())

        btnAddTimer.setOnClickListener { addNewTimer() }
        btnBack.setOnClickListener { 
            val userName = userNameInput.text.toString().trim()
            preferenceManager.setUserName(userName)
            finish() 
        }

        setupColorSelection()
        updateCustomTimersList()
    }

    private fun addNewTimer() {
        val input = timerInputField.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(this, "분 단위로 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val minutes = input.toIntOrNull()
        if (minutes == null || minutes <= 0 || minutes > 1440) {
            Toast.makeText(this, "1~1440 사이의 숫자를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTimers = preferenceManager.getCustomTimers()
        if (currentTimers.contains(minutes)) {
            Toast.makeText(this, "이미 추가된 타이머입니다", Toast.LENGTH_SHORT).show()
            return
        }

        preferenceManager.addCustomTimer(minutes)
        timerInputField.text.clear()
        updateCustomTimersList()
        Toast.makeText(this, "${minutes}분 타이머 추가됨", Toast.LENGTH_SHORT).show()
    }

    private fun updateCustomTimersList() {
        customTimersListContainer.removeAllViews()
        val customTimers = preferenceManager.getCustomTimers().sorted()

        if (customTimers.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "추가된 타이머가 없습니다.\n위에서 추가해주세요."
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 16, 0, 16) }
                textSize = 14f
            }
            customTimersListContainer.addView(emptyText)
            return
        }

        for (minutes in customTimers) {
            val itemLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }
                orientation = LinearLayout.HORIZONTAL
            }

            val timerText = TextView(this).apply {
                text = "${minutes}분"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 16f
            }

            val deleteBtn = Button(this).apply {
                text = "삭제"
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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

    override fun onResume() {
        super.onResume()
        updateCustomTimersList()
    }

    private fun setupColorSelection() {
        val currentColor = preferenceManager.getUIColor()
        
        for ((colorName, colorRes) in colors) {
            val colorButton = Button(this).apply {
                text = ""
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 4, 4, 4)
                }
                setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, colorRes))
                setMinHeight(60)
                setOnClickListener {
                    preferenceManager.setUIColor(colorName)
                    recreate()
                }
            }
            colorSelectionContainer.addView(colorButton)
        }
    }
}