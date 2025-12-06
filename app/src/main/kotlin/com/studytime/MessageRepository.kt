package com.studytime

import android.content.Context

class MessageRepository(context: Context) {
    private val messages = listOf(
        context.getString(R.string.praise_1),
        context.getString(R.string.praise_2),
        context.getString(R.string.praise_3),
        context.getString(R.string.praise_4),
        context.getString(R.string.praise_5),
        context.getString(R.string.praise_6),
        context.getString(R.string.praise_7),
        context.getString(R.string.praise_8),
        context.getString(R.string.praise_9),
        context.getString(R.string.praise_10),
        context.getString(R.string.praise_11),
        context.getString(R.string.praise_12),
        context.getString(R.string.praise_13),
        context.getString(R.string.praise_14),
        context.getString(R.string.praise_15),
        context.getString(R.string.praise_16),
        context.getString(R.string.praise_17),
        context.getString(R.string.praise_18),
        context.getString(R.string.praise_19),
        context.getString(R.string.praise_20)
    )

    fun getRandomMessage(): String {
        return messages.random()
    }
}
