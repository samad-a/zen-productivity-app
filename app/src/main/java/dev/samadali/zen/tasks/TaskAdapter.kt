package dev.samadali.zen.tasks

import android.annotation.SuppressLint
import android.graphics.Paint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.samadali.zen.R
import dev.samadali.zen.data.Task
import dev.samadali.zen.databinding.ItemCompletedHeaderBinding
import dev.samadali.zen.databinding.TaskItemBinding
import java.util.Date

class TaskAdapter(private val listener: Listener) :
    ListAdapter<TaskListItem, RecyclerView.ViewHolder>(DiffCallback) {

    interface Listener {
        fun onToggle(task: Task)
        fun onEdit(task: Task)
        fun onStartDrag(holder: RecyclerView.ViewHolder)
        /** Moves an unfinished task one place up (-1) or down (+1), for TalkBack users. */
        fun onMoveBy(task: Task, offset: Int)
        fun onToggleCompletedSection()
        fun onClearCompleted()
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is TaskListItem.TaskRow -> TYPE_TASK
        is TaskListItem.CompletedHeader -> TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TASK) TaskViewHolder(TaskItemBinding.inflate(inflater, parent, false))
        else HeaderViewHolder(ItemCompletedHeaderBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TaskListItem.TaskRow -> (holder as TaskViewHolder).bind(item.task)
            is TaskListItem.CompletedHeader -> (holder as HeaderViewHolder).bind(item)
        }
    }

    inner class TaskViewHolder(private val binding: TaskItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var task: Task? = null
            private set

        @SuppressLint("ClickableViewAccessibility") // The handle only starts a drag; TalkBack gets move actions
        fun bind(task: Task) {
            this.task = task
            val context = binding.root.context
            binding.tvTaskName.text = task.name
            binding.tvTaskName.paintFlags = if (task.isCompleted) {
                binding.tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.tvTaskDescription.text = task.description
            binding.tvTaskDescription.isVisible = task.description.isNotBlank()
            binding.categoryIcon.setImageResource(task.category.icon)

            val due = task.dueAt
            binding.tvTaskDue.isVisible = due != null && !task.isCompleted
            if (due != null) {
                binding.tvTaskDue.text = dueText(task)
                binding.tvTaskDue.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    if (task.remindAt != null) R.drawable.ic_alarm else R.drawable.ic_event, 0, 0, 0
                )
            }

            binding.checkBox.isChecked = task.isCompleted
            // TalkBack reads this with the checked state, e.g. "Mark Revise maths as done, not checked"
            binding.checkBox.contentDescription = context.getString(R.string.mark_task_complete, task.name)
            binding.checkBox.setOnClickListener { listener.onToggle(task) }
            binding.taskCard.setOnClickListener { listener.onEdit(task) }
            binding.taskCard.contentDescription = null

            // Finished tasks keep their place in the finished section, so can't be dragged
            binding.dragHandle.isVisible = !task.isCompleted
            binding.dragHandle.contentDescription = context.getString(R.string.reorder_task, task.name)
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) listener.onStartDrag(this)
                false
            }
            setMoveActions(task)
        }

        private fun setMoveActions(task: Task) {
            val card = binding.taskCard
            ViewCompat.removeAccessibilityAction(card, R.id.action_move_up)
            ViewCompat.removeAccessibilityAction(card, R.id.action_move_down)
            if (task.isCompleted) return
            val context = card.context
            ViewCompat.replaceAccessibilityAction(
                card, AccessibilityActionCompat(R.id.action_move_up, context.getString(R.string.move_up)), context.getString(R.string.move_up)
            ) { _, _ -> listener.onMoveBy(task, -1); true }
            ViewCompat.replaceAccessibilityAction(
                card, AccessibilityActionCompat(R.id.action_move_down, context.getString(R.string.move_down)), context.getString(R.string.move_down)
            ) { _, _ -> listener.onMoveBy(task, 1); true }
        }

        private fun dueText(task: Task): String {
            val context = binding.root.context
            val due = task.dueAt ?: return ""
            val date = DateFormat.getMediumDateFormat(context).format(Date(due))
            val remindAt = task.remindAt
            val text = if (remindAt != null) {
                "$date, ${DateFormat.getTimeFormat(context).format(Date(remindAt))}"
            } else {
                date
            }
            val overdue = due + DAY_MILLIS <= System.currentTimeMillis()
            return context.getString(if (overdue) R.string.task_due_overdue else R.string.due_date_value, text)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemCompletedHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TaskListItem.CompletedHeader) {
            val context = binding.root.context
            binding.completedToggle.text =
                context.resources.getQuantityString(R.plurals.completed_section, header.count, header.count)
            ViewCompat.setStateDescription(
                binding.completedToggle,
                context.getString(if (header.expanded) R.string.state_expanded else R.string.state_collapsed)
            )
            // Chevron points down when collapsed and up when expanded
            binding.completedToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (header.expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more, 0, 0, 0
            )
            binding.completedToggle.setOnClickListener { listener.onToggleCompletedSection() }
            binding.clearCompletedButton.setOnClickListener { listener.onClearCompleted() }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TaskListItem>() {
        override fun areItemsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean = when {
            oldItem is TaskListItem.TaskRow && newItem is TaskListItem.TaskRow -> oldItem.task.id == newItem.task.id
            else -> oldItem is TaskListItem.CompletedHeader && newItem is TaskListItem.CompletedHeader
        }

        override fun areContentsTheSame(oldItem: TaskListItem, newItem: TaskListItem) = oldItem == newItem
    }

    companion object {
        const val TYPE_TASK = 0
        const val TYPE_HEADER = 1
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
