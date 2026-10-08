package dev.samadali.zen.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import dev.samadali.zen.R
import dev.samadali.zen.ZenApp
import dev.samadali.zen.databinding.FragmentSettingsBinding
import dev.samadali.zen.databinding.ItemSettingRowBinding
import dev.samadali.zen.pomodoro.PomodoroTimer
import dev.samadali.zen.pomodoro.TimerSettings
import dev.samadali.zen.pomodoro.TimerSettingsSheet
import dev.samadali.zen.tasks.reminders.TaskReminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private lateinit var settings: AppSettings
    private var binding: FragmentSettingsBinding? = null

    private val exportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) export(uri)
    }

    // The daily notifications are still scheduled if refused; Android just won't show them
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { render() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)
        this.binding = binding
        settings = AppSettings(requireContext())

        row(binding.timerRow, R.string.timer_settings, getString(R.string.timer_settings_summary)) {
            TimerSettingsSheet().show(childFragmentManager, TimerSettingsSheet.TAG)
        }

        bindSwitch(binding.taskRemindersSwitch, settings.taskReminders) { settings.taskReminders = it }
        bindSwitch(binding.streakRemindersSwitch, settings.streakReminders) {
            settings.streakReminders = it
            onDailyNotificationChanged(it)
        }
        bindSwitch(binding.studyMotivationSwitch, settings.studyMotivation) {
            settings.studyMotivation = it
            onDailyNotificationChanged(it)
        }

        row(binding.exportRow, R.string.export_data, getString(R.string.export_data_summary)) {
            exportFile.launch("zen-export-${LocalDate.now()}.json")
        }
        row(binding.deleteRow, R.string.delete_all_data, getString(R.string.delete_all_data_summary)) { confirmDelete() }

        row(binding.versionRow, R.string.version, appVersion()) {}
        binding.versionRow.settingRow.isClickable = false
        binding.versionRow.settingRow.isFocusable = false
        row(binding.privacyRow, R.string.privacy, null) { showText(R.string.privacy, R.string.privacy_text) }
        row(binding.licencesRow, R.string.licences, null) { showText(R.string.licences, R.string.licences_text) }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed notification permission in system settings
        render()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val enabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        row(
            binding.notificationsRow, R.string.enable_notifications,
            getString(if (enabled) R.string.notifications_on else R.string.notifications_off)
        ) { openSystemNotificationSettings() }
        row(binding.themeRow, R.string.theme, getString(themeLabel(settings.theme))) { chooseTheme() }
    }

    private fun row(row: ItemSettingRowBinding, @StringRes title: Int, summary: String?, onClick: () -> Unit) {
        row.rowTitle.setText(title)
        row.rowSummary.text = summary
        row.rowSummary.isVisible = summary != null
        row.settingRow.setOnClickListener { onClick() }
    }

    private fun bindSwitch(switch: MaterialSwitch, initial: Boolean, onChanged: (Boolean) -> Unit) {
        switch.isChecked = initial
        switch.setOnCheckedChangeListener { _, checked -> onChanged(checked) }
    }

    private fun onDailyNotificationChanged(enabled: Boolean) {
        DailyNotifications.sync(requireContext())
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openSystemNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
        }
        startActivity(intent)
    }

    private fun chooseTheme() {
        val themes = AppSettings.Theme.entries
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme)
            .setSingleChoiceItems(
                themes.map { getString(themeLabel(it)) }.toTypedArray(),
                themes.indexOf(settings.theme)
            ) { dialog, which ->
                dialog.dismiss()
                // Recreates the activity in the new theme if it changes
                settings.theme = themes[which]
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @StringRes
    private fun themeLabel(theme: AppSettings.Theme) = when (theme) {
        AppSettings.Theme.SYSTEM -> R.string.theme_system
        AppSettings.Theme.LIGHT -> R.string.theme_light
        AppSettings.Theme.DARK -> R.string.theme_dark
    }

    private fun export(uri: Uri) {
        val app = requireContext().applicationContext as ZenApp
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val json = DataExport.toJson(
                        app.database.taskDao().getAllOnce(),
                        app.database.focusSessionDao().getAllOnce(),
                        System.currentTimeMillis()
                    )
                    app.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        ?: error("Couldn't open $uri")
                }
            }
            val message = if (result.isSuccess) R.string.export_done else R.string.export_failed
            Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_all_data_title)
            .setMessage(R.string.delete_all_data_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteAllData() }
            .show()
    }

    private fun deleteAllData() {
        val app = requireContext().applicationContext as ZenApp
        PomodoroTimer.reset()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                app.database.taskDao().getAllOnce().forEach { TaskReminders.cancel(app, it.id) }
                app.database.clearAllTables()
            }
            app.getSharedPreferences(TimerSettings.PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
            PomodoroTimer.init(app)
            Toast.makeText(app, R.string.delete_all_data_done, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showText(@StringRes title: Int, @StringRes text: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun appVersion(): String {
        val context = requireContext()
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return info.versionName ?: ""
    }
}
