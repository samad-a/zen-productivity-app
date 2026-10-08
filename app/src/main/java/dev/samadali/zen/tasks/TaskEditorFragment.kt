package dev.samadali.zen.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.samadali.zen.R
import dev.samadali.zen.data.Task
import dev.samadali.zen.data.TaskCategory
import dev.samadali.zen.databinding.FragmentTaskEditorBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/** Adds a new task, or edits an existing one when given a task id. */
class TaskEditorFragment : Fragment(R.layout.fragment_task_editor) {
    private val viewModel: TaskViewModel by activityViewModels()
    private var binding: FragmentTaskEditorBinding? = null

    private var existing: Task? = null
    private var category = TaskCategory.OTHER
    private var dueDate: LocalDate? = null
    private var reminderTime: LocalTime? = null

    // Reminders still save if notifications are refused; the user is told they won't appear
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(requireContext(), R.string.reminder_needs_notifications, Toast.LENGTH_LONG).show()
        }

    private val taskId: Long? get() = arguments?.getLong(ARG_TASK_ID)?.takeIf { it > 0 }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentTaskEditorBinding.bind(view)
        this.binding = binding
        binding.editorTitle.setText(if (taskId == null) R.string.new_task else R.string.edit_task)
        binding.addTaskButton.setText(if (taskId == null) R.string.add_task else R.string.save_task)
        addCategoryChips()

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            taskId?.let { id -> viewLifecycleOwner.lifecycleScope.launch { loadTask(id) } }
        }
        render()

        binding.categoryChips.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) } ?: return@setOnCheckedStateChangeListener
            category = chip.tag as TaskCategory
        }
        binding.dueDateButton.setOnClickListener { pickDueDate() }
        binding.clearDueDateButton.setOnClickListener {
            dueDate = null
            reminderTime = null
            render()
        }
        binding.reminderSwitch.setOnClickListener {
            if (binding.reminderSwitch.isChecked) {
                if (dueDate == null) dueDate = LocalDate.now()
                reminderTime = DEFAULT_REMINDER_TIME
                askForNotificationsIfNeeded()
                pickReminderTime()
            } else {
                reminderTime = null
            }
            render()
        }
        binding.reminderTimeButton.setOnClickListener { pickReminderTime() }
        binding.addTaskButton.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CATEGORY, category.name)
        outState.putString(KEY_DUE_DATE, dueDate?.toString())
        outState.putString(KEY_REMINDER_TIME, reminderTime?.toString())
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private suspend fun loadTask(id: Long) {
        val task = viewModel.getTask(id) ?: return
        existing = task
        val binding = binding ?: return
        binding.taskNameInput.setText(task.name)
        binding.taskDescriptionInput.setText(task.description)
        category = task.category
        val zone = ZoneId.systemDefault()
        dueDate = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        reminderTime = task.remindAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
        render()
    }

    private fun restoreState(state: Bundle) {
        category = TaskCategory.valueOf(state.getString(KEY_CATEGORY) ?: TaskCategory.OTHER.name)
        dueDate = state.getString(KEY_DUE_DATE)?.let(LocalDate::parse)
        reminderTime = state.getString(KEY_REMINDER_TIME)?.let(LocalTime::parse)
        // The rest of an existing task is reloaded; the text fields restore themselves
        taskId?.let { id -> viewLifecycleOwner.lifecycleScope.launch { existing = viewModel.getTask(id) } }
    }

    private fun addCategoryChips() {
        val binding = binding ?: return
        val dark = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.dark_green))
        for (option in TaskCategory.entries) {
            binding.categoryChips.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                tag = option
                setText(option.label)
                setChipIconResource(option.icon)
                chipIconTint = dark
                isCheckable = true
                isCheckedIconVisible = false
                setTextColor(dark)
            })
        }
    }

    private fun render() {
        val binding = binding ?: return
        for (i in 0 until binding.categoryChips.childCount) {
            val chip = binding.categoryChips.getChildAt(i) as Chip
            if (chip.tag == category && !chip.isChecked) chip.isChecked = true
        }
        val date = dueDate
        binding.dueDateButton.text = if (date == null) getString(R.string.add_due_date)
        else getString(R.string.due_date_value, date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
        binding.clearDueDateButton.isVisible = date != null

        val time = reminderTime
        binding.reminderSwitch.isChecked = time != null
        binding.reminderTimeButton.isVisible = time != null
        if (time != null) {
            val epoch = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            binding.reminderTimeButton.text =
                getString(R.string.reminder_at, DateFormat.getTimeFormat(requireContext()).format(Date(epoch)))
        }
    }

    private fun pickDueDate() {
        // The date picker works in UTC midnight milliseconds
        val initial = (dueDate ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.add_due_date)
            .setSelection(initial)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            dueDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()
            render()
        }
        picker.show(childFragmentManager, "due_date")
    }

    private fun pickReminderTime() {
        val time = reminderTime ?: DEFAULT_REMINDER_TIME
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(time.hour)
            .setMinute(time.minute)
            .setTitleText(R.string.remind_me)
            .build()
        picker.addOnPositiveButtonClickListener {
            reminderTime = LocalTime.of(picker.hour, picker.minute)
            render()
        }
        picker.show(childFragmentManager, "reminder_time")
    }

    private fun askForNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun save() {
        val binding = binding ?: return
        val name = binding.taskNameInput.text?.toString().orEmpty().trim()
        val description = binding.taskDescriptionInput.text?.toString().orEmpty().trim()
        if (name.isEmpty()) {
            binding.taskNameInput.error = getString(R.string.error_task_name_required)
            return
        }
        val zone = ZoneId.systemDefault()
        val dueAt = dueDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        val remindAt = dueDate?.let { date ->
            reminderTime?.atDate(date)?.atZone(zone)?.toInstant()?.toEpochMilli()
        }
        val task = existing
        if (task == null) {
            viewModel.addTask(name, description, category, dueAt, remindAt)
        } else {
            viewModel.updateTask(
                task.copy(name = name, description = description, category = category, dueAt = dueAt, remindAt = remindAt)
            )
        }
        parentFragmentManager.popBackStack()
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"
        private const val KEY_CATEGORY = "category"
        private const val KEY_DUE_DATE = "due_date"
        private const val KEY_REMINDER_TIME = "reminder_time"
        private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

        fun newInstance(taskId: Long?) = TaskEditorFragment().apply {
            if (taskId != null) arguments = bundleOf(ARG_TASK_ID to taskId)
        }
    }
}
