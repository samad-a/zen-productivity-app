package dev.samadali.zen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class Pomodoro : Fragment() {
    private lateinit var clockTimer: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var startStopButton: Button
    private lateinit var studyTextInput: TextInputEditText
    private lateinit var breakTextInput: TextInputEditText

    // The timer still runs if notifications are denied, it just can't alert the user.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            PomodoroTimer.start(requireContext())
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pomodoro, container, false)

        // Initialize views
        clockTimer = view.findViewById(R.id.clockTimer)
        progressBar = view.findViewById(R.id.progressBar)
        startStopButton = view.findViewById(R.id.startStopButton)
        studyTextInput = view.findViewById(R.id.studyTextInput)
        breakTextInput = view.findViewById(R.id.breakTextInput)

        // Set up observers
        PomodoroTimer.remainingMillis.observe(viewLifecycleOwner) { time ->
            updateTimerUI(time, PomodoroTimer.totalMillis.value ?: time)
        }

        PomodoroTimer.isRunning.observe(viewLifecycleOwner) { isRunning ->
            startStopButton.text = if (isRunning) "Pause" else "Start"
            studyTextInput.isEnabled = !isRunning
            breakTextInput.isEnabled = !isRunning
        }

        // Set initial values
        studyTextInput.setText(PomodoroTimer.studyMinutes.toString())
        breakTextInput.setText(PomodoroTimer.breakMinutes.toString())

        // Set up text change listeners
        studyTextInput.addTextChangedListener(createTextWatcher(true))
        breakTextInput.addTextChangedListener(createTextWatcher(false))

        // Set up click listener for start/pause button
        startStopButton.setOnClickListener {
            when {
                PomodoroTimer.isRunning.value == true -> PomodoroTimer.pause()
                needsNotificationPermission() ->
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> PomodoroTimer.start(requireContext())
            }
        }

        return view
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun createTextWatcher(isStudy: Boolean): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (PomodoroTimer.isRunning.value != true) {
                    val minutes = s.toString().toLongOrNull() ?: 0L
                    if (minutes > 0) {
                        if (isStudy) {
                            PomodoroTimer.setStudyMinutes(minutes)
                        } else {
                            PomodoroTimer.setBreakMinutes(minutes)
                        }
                    }
                }
            }
        }
    }
    private fun updateTimerUI(currentTime: Long, totalTime: Long) {
        val minutes = (currentTime / 1000) / 60
        val seconds = (currentTime / 1000) % 60
        clockTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val progress = if (totalTime > 0) ((totalTime - currentTime) * 100 / totalTime).toInt() else 0
        progressBar.progress = progress
    }
}