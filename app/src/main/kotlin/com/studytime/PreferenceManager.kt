package com.studytime

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        private const val FIRST_RUN = "first_run"
        private const val DEVICE_ADMIN_ENABLED = "device_admin_enabled"
        private const val CUSTOM_TIMERS = "custom_timers"
        private const val USER_NAME = "user_name"
        private const val TIMER_END_TIME = "timer_end_time"
        private const val TIMER_IS_RUNNING = "timer_is_running"
        private const val TIMER_REMAINING_MILLIS = "timer_remaining_millis"
        private const val UI_COLOR = "ui_color"
    }

    fun isFirstRun(): Boolean = prefs.getBoolean(FIRST_RUN, true)

    fun setFirstRunDone() {
        prefs.edit().putBoolean(FIRST_RUN, false).apply()
    }

    fun isDeviceAdminEnabled(): Boolean = prefs.getBoolean(DEVICE_ADMIN_ENABLED, false)

    fun setDeviceAdminEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DEVICE_ADMIN_ENABLED, enabled).apply()
    }

    fun getCustomTimers(): List<Int> {
        val json = prefs.getString(CUSTOM_TIMERS, "")
        return if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            json.split(",").mapNotNull { it.toIntOrNull() }
        }
    }

    fun addCustomTimer(minutes: Int) {
        val currentTimers = getCustomTimers().toMutableList()
        if (!currentTimers.contains(minutes)) {
            currentTimers.add(minutes)
            currentTimers.sort()
            saveCustomTimers(currentTimers)
        }
    }

    fun removeCustomTimer(minutes: Int) {
        val currentTimers = getCustomTimers().toMutableList()
        currentTimers.remove(minutes)
        saveCustomTimers(currentTimers)
    }

    private fun saveCustomTimers(timers: List<Int>) {
        val json = timers.joinToString(",")
        prefs.edit().putString(CUSTOM_TIMERS, json).apply()
    }

    fun getUserName(): String = prefs.getString(USER_NAME, "") ?: ""

    fun setUserName(name: String) {
        prefs.edit().putString(USER_NAME, name).apply()
    }

    fun saveTimerState(endTimeMillis: Long, isRunning: Boolean) {
        prefs.edit().apply {
            putLong(TIMER_END_TIME, endTimeMillis)
            putBoolean(TIMER_IS_RUNNING, isRunning)
            apply()
        }
    }

    fun saveTimerPausedState(remainingMillis: Long) {
        prefs.edit().apply {
            putLong(TIMER_REMAINING_MILLIS, remainingMillis)
            putLong(TIMER_END_TIME, 0L)
            putBoolean(TIMER_IS_RUNNING, false)
            apply()
        }
    }

    fun getTimerRemainingMillis(): Long = prefs.getLong(TIMER_REMAINING_MILLIS, 0L)

    fun getTimerEndTime(): Long = prefs.getLong(TIMER_END_TIME, 0L)

    fun isTimerRunning(): Boolean = prefs.getBoolean(TIMER_IS_RUNNING, false)

    fun clearTimerState() {
        prefs.edit().apply {
            putLong(TIMER_END_TIME, 0L)
            putLong(TIMER_REMAINING_MILLIS, 0L)
            putBoolean(TIMER_IS_RUNNING, false)
            apply()
        }
    }

    fun getUIColor(): String = prefs.getString(UI_COLOR, "purple") ?: "purple"

    fun setUIColor(colorName: String) {
        prefs.edit().putString(UI_COLOR, colorName).apply()
    }
}
