package dev.samadali.zen.tasks

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dev.samadali.zen.R
import dev.samadali.zen.data.Task
import dev.samadali.zen.databinding.FragmentTasksBinding

class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentTasksBinding.bind(view)

        taskAdapter = TaskAdapter(object : TaskAdapter.Listener {
            override fun onToggle(task: Task) = viewModel.toggleTaskCompletion(task)
            override fun onEdit(task: Task) = openEditor(task.id)
            override fun onStartDrag(holder: RecyclerView.ViewHolder) = touchHelper.startDrag(holder)
            override fun onMoveBy(task: Task, offset: Int) = moveBy(task, offset)
            override fun onToggleCompletedSection() = viewModel.toggleCompletedSection()
            override fun onClearCompleted() = clearCompleted(view)
        })
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.tasksRecyclerView.adapter = taskAdapter

        touchHelper = ItemTouchHelper(TouchCallback(view))
        touchHelper.attachToRecyclerView(binding.tasksRecyclerView)

        binding.addNewTaskButton.setOnClickListener { openEditor(null) }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            taskAdapter.submitList(items)
            binding.emptyText.isVisible = items.isEmpty()
        }
    }

    private fun openEditor(taskId: Long?) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, TaskEditorFragment.newInstance(taskId))
            .addToBackStack(null)
            .commit()
    }

    private fun unfinishedTasks(items: List<TaskListItem>): List<Task> =
        items.filterIsInstance<TaskListItem.TaskRow>().map { it.task }.filterNot { it.isCompleted }

    private fun moveBy(task: Task, offset: Int) {
        val tasks = unfinishedTasks(taskAdapter.currentList).toMutableList()
        val from = tasks.indexOfFirst { it.id == task.id }
        val to = from + offset
        if (from < 0 || to !in tasks.indices) return
        tasks.add(to, tasks.removeAt(from))
        viewModel.reorder(tasks)
    }

    private fun clearCompleted(view: View) {
        val cleared = viewModel.clearCompleted()
        if (cleared.isEmpty()) return
        Snackbar.make(view, resources.getQuantityString(R.plurals.tasks_cleared, cleared.size, cleared.size), Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { viewModel.restoreTasks(cleared) }
            .setAnchorView(requireActivity().findViewById(R.id.bottomNavigationView))
            .show()
    }

    /** Drag unfinished tasks by their handle to reorder; swipe any task sideways to delete it. */
    private inner class TouchCallback(private val view: View) : ItemTouchHelper.Callback() {

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val task = (viewHolder as? TaskAdapter.TaskViewHolder)?.task ?: return 0
            val drag = if (task.isCompleted) 0 else ItemTouchHelper.UP or ItemTouchHelper.DOWN
            return makeMovementFlags(drag, ItemTouchHelper.START or ItemTouchHelper.END)
        }

        // Drags start from the handle only, so long-pressing a card doesn't move it
        override fun isLongPressDragEnabled() = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val targetTask = (target as? TaskAdapter.TaskViewHolder)?.task ?: return false
            if (targetTask.isCompleted) return false
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
            val items = taskAdapter.currentList.toMutableList()
            items.add(to, items.removeAt(from))
            taskAdapter.submitList(items)
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            // Save the order once the drag ends, rather than on every step
            viewModel.reorder(unfinishedTasks(taskAdapter.currentList))
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val task = (viewHolder as? TaskAdapter.TaskViewHolder)?.task ?: return
            viewModel.deleteTask(task)
            Snackbar.make(view, R.string.task_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) { viewModel.restoreTasks(listOf(task)) }
                .setAnchorView(requireActivity().findViewById(R.id.bottomNavigationView))
                .show()
        }
    }
}
