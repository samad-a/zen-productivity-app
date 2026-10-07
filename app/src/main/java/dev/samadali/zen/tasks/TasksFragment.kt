package dev.samadali.zen.tasks

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dev.samadali.zen.R
import dev.samadali.zen.databinding.FragmentTasksBinding

class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private val viewModel: TaskViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentTasksBinding.bind(view)

        val taskAdapter = TaskAdapter { task ->
            viewModel.toggleTaskCompletion(task)
        }
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.tasksRecyclerView.adapter = taskAdapter

        // Swipe a task sideways to delete it, with an undo option
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val task = taskAdapter.currentList[position]
                viewModel.deleteTask(task)
                Snackbar.make(view, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.restoreTask(task) }
                    .setAnchorView(requireActivity().findViewById(R.id.bottomNavigationView))
                    .show()
            }
        }).attachToRecyclerView(binding.tasksRecyclerView)

        binding.addNewTaskButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.flFragment, AddTaskFragment())
                .addToBackStack(null)
                .commit()
        }

        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
    }
}
