package dev.samadali.zen.stats

import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.samadali.zen.R
import dev.samadali.zen.databinding.FragmentStatsBinding
import dev.samadali.zen.databinding.ItemStatBinding
import java.time.format.TextStyle
import java.util.Locale

class StatsFragment : Fragment(R.layout.fragment_stats) {
    private val viewModel: ActivityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentStatsBinding.bind(view)
        HeatColors.bindLegend(binding.legend.root as LinearLayout)

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            bindStat(binding.tasksCompleted, summary.tasksCompleted.toString(), getString(R.string.tasks_completed))
            bindStat(binding.hoursFocused, formatHours(summary.focusMillis), getString(R.string.hours_focused))
            bindStat(
                binding.longestStreak, summary.longestStreak.toString(),
                resources.getQuantityString(R.plurals.longest_streak_label, summary.longestStreak)
            )
            val days = summary.daysSinceJoining.toInt()
            bindStat(
                binding.daysSinceJoining, days.toString(),
                resources.getQuantityString(R.plurals.days_since_joining_label, days)
            )

            binding.mostProductiveText.text = summary.mostProductiveDay?.let {
                getString(R.string.most_productive_day, it.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            } ?: getString(R.string.most_productive_day_unknown)
            binding.uniqueTasksText.text =
                resources.getQuantityString(R.plurals.unique_tasks, summary.uniqueTasks, summary.uniqueTasks)
        }

        viewModel.byDay.observe(viewLifecycleOwner) { byDay ->
            binding.yearHeatmap.setActivity(byDay)
            // Start at the most recent weeks, at the end of the scroll
            binding.yearScroll.post { binding.yearScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }
        }
    }

    private fun bindStat(stat: ItemStatBinding, value: String, label: String) {
        stat.statValue.text = value
        stat.statLabel.text = label
        stat.statContent.contentDescription = getString(R.string.stat_description, value, label)
    }

    /** Hours to one decimal place, e.g. "12.5". */
    private fun formatHours(millis: Long): String =
        String.format(Locale.getDefault(), "%.1f", millis / 3_600_000.0)
}
