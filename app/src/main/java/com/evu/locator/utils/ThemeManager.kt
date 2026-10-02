package com.evu.locator.utils

import android.content.Context

object ThemeManager {
    private const val PREFS = "locator_theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val LIGHT_BLUE = 0
    const val LIGHT_GREEN = 1
    const val DARK_BLUE = 2
    const val DARK_GREEN = 3

    fun getThemeResId(context: Context): Int {
        return when (getSavedTheme(context)) {
            LIGHT_GREEN -> com.evu.locator.R.style.Theme_LightGreen
            DARK_BLUE -> com.evu.locator.R.style.Theme_DarkBlue
            DARK_GREEN -> com.evu.locator.R.style.Theme_DarkGreen
            else -> com.evu.locator.R.style.Theme_LightBlue
        }
    }

    fun getSavedTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, LIGHT_BLUE)
    }

    fun setTheme(context: Context, theme: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, theme).apply()
    }
}