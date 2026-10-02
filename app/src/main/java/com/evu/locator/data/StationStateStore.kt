package com.evu.locator.data

import android.content.Context

// Tracks which stations we're currently "inside", shared between geofence receiver
// and the periodic location checker to avoid duplicate enter/exit posts.
object StationStateStore {
    private const val PREFS = "locator_station_state"
    private const val KEY_INSIDE = "inside_stations"

    fun getInsideSet(context: Context): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_INSIDE, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    fun saveInsideSet(context: Context, set: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_INSIDE, set).apply()
    }

    fun markEntered(context: Context, stationId: String) {
        val set = getInsideSet(context)
        set.add(stationId)
        saveInsideSet(context, set)
    }

    fun markExited(context: Context, stationId: String) {
        val set = getInsideSet(context)
        set.remove(stationId)
        saveInsideSet(context, set)
    }
}