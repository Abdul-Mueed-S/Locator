package com.evu.locator.notifications

import android.content.Context

object LastSeenStore {
    private const val PREFS = "locator_last_seen"

    fun get(context: Context, contactUid: String): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(contactUid, 0L)
    }

    fun set(context: Context, contactUid: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong(contactUid, timestamp).apply()
    }
}