package com.studytime

import android.content.Context
import androidx.core.content.ContextCompat

object ColorUtils {
    private val colorMap = mapOf(
        "purple" to R.color.purple_500,
        "blue" to R.color.blue_500,
        "red" to R.color.red_500,
        "green" to R.color.green_500,
        "orange" to R.color.orange_500,
        "pink" to R.color.pink_500,
        "teal" to R.color.teal_700
    )

    fun getPrimaryColor(context: Context, colorName: String): Int {
        val colorRes = colorMap[colorName] ?: R.color.purple_500
        return ContextCompat.getColor(context, colorRes)
    }

    fun getSecondaryColor(context: Context, colorName: String): Int {
        val secondaryColorMap = mapOf(
            "purple" to R.color.purple_700,
            "blue" to R.color.blue_700,
            "red" to R.color.red_700,
            "green" to R.color.green_700,
            "orange" to R.color.orange_700,
            "pink" to R.color.pink_700,
            "teal" to R.color.teal_200
        )
        val colorRes = secondaryColorMap[colorName] ?: R.color.purple_700
        return ContextCompat.getColor(context, colorRes)
    }
}
