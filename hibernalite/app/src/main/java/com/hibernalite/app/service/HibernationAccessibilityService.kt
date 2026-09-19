package com.hibernalite.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.hibernalite.app.R
import java.util.LinkedList
import java.util.Queue

class HibernationAccessibilityService : AccessibilityService() {

    companion object {
        var isRunning: Boolean = false
            private set

        private val hibernationQueue: Queue<String> = LinkedList()
        private var currentProcessingPackage: String? = null
        private var isServiceActive = false

        fun isAccessibilityEnabled(context: Context): Boolean {
            return isServiceActive
        }

        fun startHibernation(context: Context, packages: List<String>) {
            hibernationQueue.clear()
            hibernationQueue.addAll(packages)
            isRunning = true

            processNextApp(context)
        }

        private fun processNextApp(context: Context) {
            if (hibernationQueue.isEmpty()) {
                isRunning = false
                currentProcessingPackage = null
                Toast.makeText(context, R.string.toast_hibernation_complete, Toast.LENGTH_LONG).show()

                // Bring app back to front
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                return
            }

            val pkg = hibernationQueue.poll()
            currentProcessingPackage = pkg

            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$pkg")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // If opening details fails, continue to next
                processNextApp(context)
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isActionPending = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
        isRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRunning || event == null) return

        val rootNode = rootInActiveWindow ?: return
        val currentPkg = currentProcessingPackage ?: return

        // We are on Settings app
        if (event.packageName?.toString() == "com.android.settings") {
            handleSettingsScreen(rootNode, currentPkg)
        }
    }

    private fun handleSettingsScreen(rootNode: AccessibilityNodeInfo, currentPkg: String) {
        if (isActionPending) return

        // Look for Force Stop button (English & Indonesian)
        val forceStopLabels = listOf("Force stop", "FORCE STOP", "Paksa berhenti", "Paksa Berhenti")
        var forceStopClicked = false

        for (label in forceStopLabels) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (node.isEnabled) {
                    isActionPending = true
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    forceStopClicked = true

                    // Look for Confirmation Dialog ("OK" / "Oke" / "Force stop")
                    mainHandler.postDelayed({
                        confirmDialogAndProceed()
                    }, 400)
                    break
                } else {
                    // Button disabled means app is already stopped
                    skipToNextApp()
                    return
                }
            }
            if (forceStopClicked) break
        }

        // If no force stop button detected within 1.5 seconds, skip to next app
        mainHandler.postDelayed({
            if (isRunning && currentProcessingPackage == currentPkg && !forceStopClicked) {
                skipToNextApp()
            }
        }, 1500)
    }

    private fun confirmDialogAndProceed() {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val confirmLabels = listOf("OK", "Ok", "Oke", "Force stop", "Paksa berhenti")
            for (label in confirmLabels) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(label)
                for (node in nodes) {
                    if (node.isClickable && node.isEnabled) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        break
                    }
                }
            }
        }

        mainHandler.postDelayed({
            isActionPending = false
            processNextApp(this)
        }, 300)
    }

    private fun skipToNextApp() {
        isActionPending = false
        processNextApp(this)
    }

    override fun onInterrupt() {
        isRunning = false
    }
}
