package dev.samadali.zen.pomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import dev.samadali.zen.R
import dev.samadali.zen.databinding.FragmentPomodoroBinding

class PomodoroFragment : Fragment(R.layout.fragment_pomodoro) {

    // The timer still runs if notifications are denied, it just can't alert the user.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            PomodoroTimer.start(requireContext())
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentPomodoroBinding.bind(view)

        PomodoroTimer.remainingMillis.observe(viewLifecycleOwner) { remaining ->
            val total = PomodoroTimer.totalMillis.value ?: remaining
            binding.clockTimer.text = formatRemaining(remaining)
            binding.progressBar.progress = progressPercent(remaining, total)
        }

        PomodoroTimer.isRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.startStopButton.setText(if (isRunning) R.string.pause else R.string.start)
            binding.studyTextInput.isEnabled = !isRunning
            binding.breakTextInput.isEnabled = !isRunning
        }

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
    }

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
