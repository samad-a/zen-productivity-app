package dev.samadali.zen.tasks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.samadali.zen.R
import dev.samadali.zen.data.TaskCategory

@get:DrawableRes
val TaskCategory.icon: Int
    get() = when (this) {
        TaskCategory.STUDY -> R.drawable.ic_category_study
        TaskCategory.WORK -> R.drawable.ic_category_work
        TaskCategory.CODING -> R.drawable.ic_category_coding
        TaskCategory.GYM -> R.drawable.ic_category_gym
        TaskCategory.READING -> R.drawable.ic_category_reading
        TaskCategory.CHORES -> R.drawable.ic_category_chores
        TaskCategory.OTHER -> R.drawable.ic_category_other
    }

@get:StringRes
val TaskCategory.label: Int
    get() = when (this) {
        TaskCategory.STUDY -> R.string.category_study
        TaskCategory.WORK -> R.string.category_work
        TaskCategory.CODING -> R.string.category_coding
        TaskCategory.GYM -> R.string.category_gym
        TaskCategory.READING -> R.string.category_reading
        TaskCategory.CHORES -> R.string.category_chores
        TaskCategory.OTHER -> R.string.category_other
    }
