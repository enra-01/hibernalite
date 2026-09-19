package com.hibernalite.app.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.hibernalite.app.model.AppInfo
import java.util.Locale

object MemoryHelper {

    data class RamStatus(
        val totalBytes: Long,
        val availableBytes: Long,
        val usedBytes: Long,
        val usedPercentage: Int
    )

    /**
     * Get real-time RAM statistics of the device.
     */
    fun getRamStatus(context: Context): RamStatus {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem
        val available = memInfo.availMem
        val used = total - available
        val percentage = if (total > 0) ((used * 100) / total).toInt() else 0

        return RamStatus(
            totalBytes = total,
            availableBytes = available,
            usedBytes = used,
            usedPercentage = percentage.coerceIn(0, 100)
        )
    }

    /**
     * Format byte values to readable String (MB / GB).
     */
    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)

        return if (gb >= 1.0) {
            String.format(Locale.getDefault(), "%.2f GB", gb)
        } else {
            String.format(Locale.getDefault(), "%.0f MB", mb)
        }
    }

    /**
     * Safely list non-critical user-installed applications.
     */
    fun getInstalledUserApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val selfPackage = context.packageName

        // Safe exclusions list (never kill system critical apps)
        val systemExclusions = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
            "com.realme.launcher",
            "com.coloros.launcher",
            "com.oppo.launcher"
        )

        val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfo>()

        for (app in installedPackages) {
            val pkg = app.packageName
            if (pkg == selfPackage || systemExclusions.contains(pkg)) {
                continue
            }

            // Exclude system apps unless updated by user
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            if (!isSystem || isUpdatedSystem) {
                val label = pm.getApplicationLabel(app).toString()
                val icon = try {
                    pm.getApplicationIcon(app)
                } catch (e: Exception) {
                    null
                }
                appList.add(
                    AppInfo(
                        appName = label,
                        packageName = pkg,
                        icon = icon,
                        isSelected = true,
                        isSystemApp = isSystem
                    )
                )
            }
        }

        return appList.sortedBy { it.appName.lowercase(Locale.getDefault()) }
    }

    /**
     * Quick clean: Release RAM cache of background processes.
     */
    fun quickClean(context: Context, packages: List<String>) {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (pkg in packages) {
            try {
                actManager.killBackgroundProcesses(pkg)
            } catch (e: Exception) {
                // Ignore safe errors
            }
        }
    }
}
