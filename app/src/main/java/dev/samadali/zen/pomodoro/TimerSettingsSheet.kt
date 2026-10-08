package dev.samadali.zen.pomodoro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import dev.samadali.zen.R
import dev.samadali.zen.databinding.SheetTimerSettingsBinding

/**
 * The less frequently changed timer options. Study and break lengths stay on the
 * pomodoro screen itself, since they're changed most often.
 */
class TimerSettingsSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetTimerSettingsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetTimerSettingsBinding.bind(view)
        val settings = PomodoroTimer.settings

        bindSlider(
            binding.longBreakSlider, binding.longBreakLabel, R.string.long_break_length,
            settings.longBreakMinutes.toInt(), R.plurals.minutes_value
        ) { PomodoroTimer.setLongBreakMinutes(it.toLong()) }
        bindSlider(
            binding.sessionsSlider, binding.sessionsLabel, R.string.sessions_before_long_break,
            settings.sessionsPerLongBreak, R.plurals.sessions_value
        ) { settings.sessionsPerLongBreak = it }
        bindSlider(
            binding.dailyGoalSlider, binding.dailyGoalLabel, R.string.daily_goal,
            settings.dailyGoal, R.plurals.sessions_value
        ) { settings.dailyGoal = it }

        bindSwitch(binding.autoStartBreaksSwitch, settings.autoStartBreaks) { settings.autoStartBreaks = it }
        bindSwitch(binding.autoStartStudySwitch, settings.autoStartStudy) { settings.autoStartStudy = it }
        bindSwitch(binding.soundSwitch, settings.sound) { settings.sound = it }
        bindSwitch(binding.vibrateSwitch, settings.vibrate) { settings.vibrate = it }
        bindSwitch(binding.keepScreenOnSwitch, settings.keepScreenOn) { settings.keepScreenOn = it }
    }

    /** Shows the slider's value in its label, e.g. "Daily goal: 8 sessions". */
    private fun bindSlider(
        slider: Slider,
        label: TextView,
        @StringRes title: Int,
        initial: Int,
        @PluralsRes unit: Int,
        onChanged: (Int) -> Unit
    ) {
        fun valueText(value: Int) = resources.getQuantityString(unit, value, value)
        fun update(value: Int) {
            label.text = getString(R.string.setting_value, getString(title), valueText(value))
        }

        slider.value = initial.toFloat().coerceIn(slider.valueFrom, slider.valueTo)
        update(slider.value.toInt())
        // Spoken by TalkBack and shown in the bubble while dragging
        slider.setLabelFormatter { valueText(it.toInt()) }
        slider.addOnChangeListener { _, value, fromUser ->
            update(value.toInt())
            if (fromUser) onChanged(value.toInt())
        }
    }

    private fun bindSwitch(switch: MaterialSwitch, initial: Boolean, onChanged: (Boolean) -> Unit) {
        switch.isChecked = initial
        switch.setOnCheckedChangeListener { _, checked -> onChanged(checked) }
    }

    companion object {
        const val TAG = "TimerSettingsSheet"
    }
}
