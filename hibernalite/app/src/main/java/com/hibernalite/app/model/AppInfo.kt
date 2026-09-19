package com.hibernalite.app.model

import android.graphics.drawable.Drawable

/**
 * Data class representing an application detected on the device.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    var isSelected: Boolean = true,
    val isSystemApp: Boolean = false
)
