package dev.samadali.zen.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import dev.samadali.zen.R
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * GitHub-style grid of the last year: one column per week, one square per day, shaded by
 * study sessions. It's a picture for sighted users; [contentDescription] gives the summary.
 */
class YearHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cellSize = HeatColors.dp(context, 12f)
    private val gap = HeatColors.dp(context, 3f)
    private val corner = HeatColors.dp(context, 2f)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = HeatColors.dp(context, 1f)
        color = ContextCompat.getColor(context, R.color.outline)
    }
    private val rect = RectF()

    private var sessionsByDay: Map<LocalDate, Int> = emptyMap()
    private var today: LocalDate = LocalDate.now()

    /** Weeks shown, ending with the current week. */
    private val weeks = 53

    fun setActivity(byDay: Map<LocalDate, DayActivity>, today: LocalDate = LocalDate.now()) {
        sessionsByDay = byDay.mapValues { it.value.sessions }
        this.today = today
        val start = firstDay()
        val studyDays = byDay.count { (day, activity) -> activity.isStudyDay && !day.isBefore(start) }
        contentDescription = resources.getQuantityString(R.plurals.year_heatmap_description, studyDays, studyDays)
        invalidate()
    }

    private fun firstDay(): LocalDate {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val startOfThisWeek = today.minusDays(((today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong())
        return startOfThisWeek.minusWeeks((weeks - 1).toLong())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = paddingLeft + paddingRight + weeks * (cellSize + gap) - gap
        val height = paddingTop + paddingBottom + 7 * (cellSize + gap) - gap
        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val start = firstDay()
        for (week in 0 until weeks) {
            for (weekday in 0 until 7) {
                val day = start.plusDays((week * 7 + weekday).toLong())
                if (day.isAfter(today)) return
                val level = ActivityStats.heatLevel(sessionsByDay[day] ?: 0)
                val left = paddingLeft + week * (cellSize + gap)
                val top = paddingTop + weekday * (cellSize + gap)
                rect.set(left, top, left + cellSize, top + cellSize)
                fillPaint.color = HeatColors.fill(context, level)
                canvas.drawRoundRect(rect, corner, corner, fillPaint)
                if (level == 0) canvas.drawRoundRect(rect, corner, corner, outlinePaint)
            }
        }
    }
}
