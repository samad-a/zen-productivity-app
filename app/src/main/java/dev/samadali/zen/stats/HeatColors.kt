package dev.samadali.zen.stats

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import dev.samadali.zen.R

/** Colours for the calendar and yearly heatmaps, by [ActivityStats.heatLevel]. */
object HeatColors {
    private val FILLS = intArrayOf(R.color.heat_0, R.color.heat_1, R.color.heat_2, R.color.heat_3)

    @ColorInt
    fun fill(context: Context, level: Int): Int = ContextCompat.getColor(context, FILLS[level])

    /** Text on a shade, chosen to keep at least 4.5:1 contrast. */
    @ColorInt
    fun text(context: Context, level: Int): Int =
        ContextCompat.getColor(context, if (level >= 3) R.color.white else R.color.dark_green)

    /** Rounded square for a day; empty days get an outline so they're still visible. */
    fun cell(context: Context, level: Int, cornerPx: Float, outlined: Boolean = level == 0): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = cornerPx
            setColor(fill(context, level))
            if (outlined) setStroke(dp(context, 1f).toInt(), ContextCompat.getColor(context, R.color.outline))
        }

    /** Fills [legend] with a swatch and label for each shade. */
    fun bindLegend(legend: LinearLayout) {
        val context = legend.context
        legend.removeAllViews()
        val labels = intArrayOf(R.string.legend_none, R.string.legend_one, R.string.legend_some, R.string.legend_many)
        labels.forEachIndexed { level, label ->
            val size = dp(context, 16f).toInt()
            legend.addView(View(context).apply {
                background = cell(context, level, dp(context, 4f))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(size, size).apply { marginStart = dp(context, 12f).toInt() })
            legend.addView(TextView(context).apply {
                setText(label)
                setTextColor(ContextCompat.getColor(context, R.color.dark_green))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.text_body))
                gravity = Gravity.CENTER_VERTICAL
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(context, 4f).toInt() })
        }
    }

    fun dp(context: Context, value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)
}

/** Formats a focus duration as "1 h 15 min" or "25 min". */
fun formatFocus(context: Context, millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) context.getString(R.string.duration_hours_minutes, hours.toInt(), minutes.toInt())
    else context.getString(R.string.duration_minutes, minutes.toInt())
}
