package dev.samadali.zen.calendar

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.samadali.zen.R
import dev.samadali.zen.databinding.FragmentCalendarBinding
import dev.samadali.zen.databinding.ItemCalendarDayBinding
import dev.samadali.zen.stats.ActivityStats
import dev.samadali.zen.stats.ActivityViewModel
import dev.samadali.zen.stats.DayActivity
import dev.samadali.zen.stats.HeatColors
import dev.samadali.zen.stats.formatFocus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** A month of days shaded by study sessions, with the selected day's stats underneath. */
class CalendarFragment : Fragment(R.layout.fragment_calendar) {
    private val viewModel: ActivityViewModel by activityViewModels()

    private var binding: FragmentCalendarBinding? = null
    private var month: YearMonth = YearMonth.now()
    private var selected: LocalDate = LocalDate.now()
    private var activity: Map<LocalDate, DayActivity> = emptyMap()
    private val adapter = DayAdapter { day ->
        selected = day
        render()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCalendarBinding.bind(view)
        this.binding = binding
        savedInstanceState?.let {
            month = YearMonth.parse(it.getString(KEY_MONTH))
            selected = LocalDate.parse(it.getString(KEY_SELECTED))
        }

        addWeekdayInitials(binding.weekdayRow)
        binding.daysGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.daysGrid.adapter = adapter
        HeatColors.bindLegend(binding.legend.root as LinearLayout)

        binding.previousMonthButton.setOnClickListener { month = month.minusMonths(1); render() }
        binding.nextMonthButton.setOnClickListener { month = month.plusMonths(1); render() }

        viewModel.byDay.observe(viewLifecycleOwner) {
            activity = it
            render()
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MONTH, month.toString())
        outState.putString(KEY_SELECTED, selected.toString())
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun render() {
        val binding = binding ?: return
        val locale = Locale.getDefault()
        binding.monthText.text = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        binding.nextMonthButton.isEnabled = month < YearMonth.now()

        val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
        val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val days = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        adapter.update(days, activity, selected)

        val day = activity[selected] ?: DayActivity()
        binding.selectedDayText.text = selected.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))
        binding.daySessionsText.text =
            resources.getQuantityString(R.plurals.study_sessions, day.sessions, day.sessions)
        binding.dayFocusText.text = getString(R.string.focused_duration, formatFocus(requireContext(), day.focusMillis))
        binding.dayTasksText.text =
            resources.getQuantityString(R.plurals.tasks_finished, day.tasksCompleted, day.tasksCompleted)
    }

    private fun addWeekdayInitials(row: LinearLayout) {
        row.removeAllViews()
        val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        for (i in 0 until 7) {
            row.addView(TextView(requireContext()).apply {
                text = first.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault())
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_body))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private object ButtonRole : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = Button::class.java.name
        }
    }

    private class DayAdapter(private val onDayClick: (LocalDate) -> Unit) :
        RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

        private var days: List<LocalDate?> = emptyList()
        private var activity: Map<LocalDate, DayActivity> = emptyMap()
        private var selected: LocalDate? = null

        fun update(days: List<LocalDate?>, activity: Map<LocalDate, DayActivity>, selected: LocalDate) {
            this.days = days
            this.activity = activity
            this.selected = selected
            @Suppress("NotifyDataSetChanged") // At most 42 cells, all of which can change
            notifyDataSetChanged()
        }

        override fun getItemCount() = days.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DayViewHolder(ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) = holder.bind(days[position])

        inner class DayViewHolder(private val binding: ItemCalendarDayBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(day: LocalDate?) {
                val view = binding.dayText
                if (day == null) {
                    view.text = null
                    view.background = null
                    view.contentDescription = null
                    view.isClickable = false
                    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    return
                }
                val context = view.context
                val sessions = activity[day]?.sessions ?: 0
                val level = ActivityStats.heatLevel(sessions)
                val isToday = day == LocalDate.now()
                val isFuture = day.isAfter(LocalDate.now())

                view.text = day.dayOfMonth.toString()
                view.setTextColor(HeatColors.text(context, level))
                view.paint.isFakeBoldText = isToday
                view.background = HeatColors.cell(context, level, HeatColors.dp(context, 8f), outlined = false).apply {
                    // Selected: thick dark outline. Today: bold number. Empty: thin grey outline.
                    val strokeColor = ContextCompat.getColor(context, if (day == selected) R.color.dark_green else R.color.outline)
                    val strokeDp = if (day == selected) 3f else if (level == 0) 1f else 0f
                    if (strokeDp > 0) setStroke(HeatColors.dp(context, strokeDp).toInt(), strokeColor)
                }
                view.alpha = if (isFuture) 0.5f else 1f

                val dateText = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                val sessionsText = context.resources.getQuantityString(R.plurals.study_sessions, sessions, sessions)
                view.contentDescription = context.getString(R.string.calendar_day_description, dateText, sessionsText)
                view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                view.isEnabled = !isFuture
                view.isSelected = day == selected
                // Announced as a button, so TalkBack users know they can select the day
                ViewCompat.setAccessibilityDelegate(view, ButtonRole)
                view.setOnClickListener { onDayClick(day) }
            }
        }
    }

    private companion object {
        const val KEY_MONTH = "month"
        const val KEY_SELECTED = "selected"
    }
}
