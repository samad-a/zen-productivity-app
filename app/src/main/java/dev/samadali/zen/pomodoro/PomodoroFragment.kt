package dev.samadali.zen.pomodoro

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dev.samadali.zen.R
import dev.samadali.zen.databinding.FragmentPomodoroBinding

class PomodoroFragment : Fragment(R.layout.fragment_pomodoro) {
    private val viewModel: PomodoroViewModel by viewModels()
    private var binding: FragmentPomodoroBinding? = null

    // The timer still runs if notifications are denied, it just can't alert the user.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            PomodoroTimer.start(requireContext())
        }

    // The timer settings sheet changes the goal and cycle length while this screen is open
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        updatePhaseText()
        updateTodayText()
        updateKeepScreenOn()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPomodoroBinding.bind(view)
        this.binding = binding

        // The ring's size is a dimension, not a layout size, so match it to the square frame
        binding.timerFrame.doOnLayout { frame ->
            binding.progressBar.indicatorSize = minOf(frame.width, frame.height)
        }

        PomodoroTimer.remainingMillis.observe(viewLifecycleOwner) { remaining ->
            val total = PomodoroTimer.totalMillis.value ?: remaining
            binding.clockTimer.text = formatRemaining(remaining)
            binding.progressBar.progress = progressPercent(remaining, total)
        }

        PomodoroTimer.isRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.startStopButton.setText(if (isRunning) R.string.pause else R.string.start)
            binding.studyTextInput.isEnabled = !isRunning
            binding.breakTextInput.isEnabled = !isRunning
            updateKeepScreenOn()
        }

        PomodoroTimer.phase.observe(viewLifecycleOwner) { updatePhaseText() }
        PomodoroTimer.completedInCycle.observe(viewLifecycleOwner) { updatePhaseText() }
        viewModel.sessionsToday.observe(viewLifecycleOwner) { updateTodayText() }

        bindDurationInput(binding.studyTextInput, PomodoroTimer.studyMinutes, PomodoroTimer::setStudyMinutes)
        bindDurationInput(binding.breakTextInput, PomodoroTimer.breakMinutes, PomodoroTimer::setBreakMinutes)

        binding.startStopButton.setOnClickListener {
            when {
                PomodoroTimer.isRunning.value == true -> PomodoroTimer.pause()
                needsNotificationPermission() ->
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> PomodoroTimer.start(requireContext())
            }
        }
        binding.resetButton.setOnClickListener { PomodoroTimer.reset() }
        binding.skipButton.setOnClickListener { PomodoroTimer.skip(requireContext()) }
        binding.timerSettingsButton.setOnClickListener {
            TimerSettingsSheet().show(childFragmentManager, TimerSettingsSheet.TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        prefs().registerOnSharedPreferenceChangeListener(settingsListener)
    }

    override fun onStop() {
        prefs().unregisterOnSharedPreferenceChangeListener(settingsListener)
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun prefs() =
        requireContext().getSharedPreferences(TimerSettings.PREFS_NAME, Context.MODE_PRIVATE)

    private fun updatePhaseText() {
        val binding = binding ?: return
        val settings = PomodoroTimer.settings
        binding.phaseText.text = when (PomodoroTimer.phase.value) {
            Phase.BREAK -> getString(R.string.short_break)
            Phase.LONG_BREAK -> getString(R.string.long_break)
            else -> {
                val session = (PomodoroTimer.completedInCycle.value ?: 0) + 1
                getString(R.string.session_x_of_y, session.coerceAtMost(settings.sessionsPerLongBreak), settings.sessionsPerLongBreak)
            }
        }
    }

    private fun updateTodayText() {
        val binding = binding ?: return
        val done = viewModel.sessionsToday.value ?: 0
        val goal = PomodoroTimer.settings.dailyGoal
        binding.todayText.text = resources.getQuantityString(R.plurals.sessions_today, goal, done, goal)
    }

    private fun updateKeepScreenOn() {
        val binding = binding ?: return
        binding.root.keepScreenOn = PomodoroTimer.isRunning.value == true && PomodoroTimer.settings.keepScreenOn
    }

    // Plain ASCII digits on purpose, so the value parses back with toLongOrNull
    @SuppressLint("SetTextI18n")
    private fun bindDurationInput(input: EditText, initialMinutes: Long, onChanged: (Long) -> Unit) {
        input.setText(initialMinutes.toString())
        input.doAfterTextChanged { text ->
            val minutes = text.toString().toLongOrNull() ?: 0L
            if (minutes > 0 && PomodoroTimer.isRunning.value != true) onChanged(minutes)
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
}
