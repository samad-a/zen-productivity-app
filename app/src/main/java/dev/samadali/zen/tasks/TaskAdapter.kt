package dev.samadali.zen.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.samadali.zen.R
import dev.samadali.zen.data.Task
import dev.samadali.zen.databinding.TaskItemBinding

class TaskAdapter(private val onTaskClick: (Task) -> Unit) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = TaskItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: TaskItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.tvTaskName.text = task.name
            binding.tvTaskDescription.text = task.description
            binding.tvTaskDescription.isVisible = task.description.isNotBlank()
            binding.checkBox.isChecked = task.isCompleted
            // TalkBack reads this with the checked state, e.g. "Mark Revise maths as done, not checked"
            binding.checkBox.contentDescription =
                binding.root.context.getString(R.string.mark_task_complete, task.name)

            binding.checkBox.setOnClickListener {
                onTaskClick(task)
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
