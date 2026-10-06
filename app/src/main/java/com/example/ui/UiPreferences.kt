package com.example.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small, device-level look preferences. They are not part of the database, so a backup
 * restore of the readings never changes how the app looks.
 */
object UiPreferences {

    private const val FILE = "ui_prefs"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    private val _dynamicColor = MutableStateFlow(false)

    /** Material You: take the colours from the wallpaper (Android 12+). Off by default. */
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun load(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        _dynamicColor.value = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        _dynamicColor.value = enabled
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
    }
}
