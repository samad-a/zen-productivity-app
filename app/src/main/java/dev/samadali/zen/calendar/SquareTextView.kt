package dev.samadali.zen.calendar

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/** A text view as tall as it is wide, for calendar day cells. */
class SquareTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        // Stay at least the minimum touch target height on very narrow screens
        if (measuredHeight < minimumHeight) setMeasuredDimension(measuredWidth, minimumHeight)
    }
}
