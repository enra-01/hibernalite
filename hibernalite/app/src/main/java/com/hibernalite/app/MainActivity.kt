package com.hibernalite.app

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hibernalite.app.adapter.AppListAdapter
import com.hibernalite.app.databinding.ActivityMainBinding
import com.hibernalite.app.model.AppInfo
import com.hibernalite.app.service.HibernationAccessibilityService
import com.hibernalite.app.util.MemoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appAdapter: AppListAdapter
    private var appsList: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        refreshRamInfo()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        refreshRamInfo()
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(emptyList()) { count ->
            updateSelectedAppsCount(count)
        }
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            refreshRamInfo()
            loadApps()
        }

        binding.btnQuickClean.setOnClickListener {
            performQuickClean()
        }

        binding.btnHibernateAll.setOnClickListener {
            checkAccessibilityAndHibernate()
        }
    }

    /**
     * Updates RAM information display and changes bar color based on load.
     */
    private fun refreshRamInfo() {
        val status = MemoryHelper.getRamStatus(this)

        binding.tvRamPercent.text = "${status.usedPercentage}%"
        binding.progressBarRam.progress = status.usedPercentage

        val usedStr = MemoryHelper.formatBytes(status.usedBytes)
        val totalStr = MemoryHelper.formatBytes(status.totalBytes)
        val availStr = MemoryHelper.formatBytes(status.availableBytes)

        binding.tvRamDetails.text = getString(R.string.ram_details_format, usedStr, totalStr, availStr)

        // Color indicator based on RAM load
        val colorRes = when {
            status.usedPercentage >= 85 -> R.color.ram_red
            status.usedPercentage >= 70 -> R.color.ram_orange
            else -> R.color.ram_green
        }
        val color = ContextCompat.getColor(this, colorRes)
        binding.tvRamPercent.setTextColor(color)
        binding.progressBarRam.progressTintList = ColorStateList.valueOf(color)
    }

    /**
     * Loads installed non-system apps in background coroutine.
     */
    private fun loadApps() {
        lifecycleScope.launch {
            binding.tvEmptyState.visibility = View.GONE

            val apps = withContext(Dispatchers.IO) {
                MemoryHelper.getInstalledUserApps(this@MainActivity)
            }

            appsList = apps
            appAdapter.updateData(apps)

            if (apps.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun updateSelectedAppsCount(count: Int) {
        binding.tvAppsCount.text = getString(R.string.apps_count_format, count)
        binding.btnHibernateAll.isEnabled = count > 0
    }

    /**
     * 1-Tap Quick Clean: releases cached RAM processes.
     */
    private fun performQuickClean() {
        val selectedPackages = appAdapter.getSelectedApps().map { it.packageName }
        MemoryHelper.quickClean(this, selectedPackages)
        Toast.makeText(this, R.string.toast_quick_clean_done, Toast.LENGTH_SHORT).show()
        refreshRamInfo()
    }

    /**
     * Checks if Accessibility Service is enabled; if so, starts automated hibernation.
     */
    private fun checkAccessibilityAndHibernate() {
        val selectedPackages = appAdapter.getSelectedApps().map { it.packageName }
        if (selectedPackages.isEmpty()) {
            Toast.makeText(this, "Pilih setidaknya 1 aplikasi untuk dihibernasi", Toast.LENGTH_SHORT).show()
            return
        }

        if (isAccessibilityServiceEnabled(this)) {
            Toast.makeText(this, R.string.toast_hibernating, Toast.LENGTH_SHORT).show()
            HibernationAccessibilityService.startHibernation(this, selectedPackages)
        } else {
            showAccessibilityDialog()
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${HibernationAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(expectedServiceName) || HibernationAccessibilityService.isAccessibilityEnabled(context)
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.enable_accessibility_title)
            .setMessage(R.string.enable_accessibility_desc)
            .setPositiveButton(R.string.btn_open_settings) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Tidak dapat membuka pengaturan aksesibilitas secara langsung.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
