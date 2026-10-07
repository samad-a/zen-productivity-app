package dev.samadali.zen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class Tasks : Fragment() {
    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var addNewTaskButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        // Initialize RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.tasksRecyclerView)
        taskAdapter = TaskAdapter { task ->
            viewModel.toggleTaskCompletion(task)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = taskAdapter

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
                val task = taskAdapter.getItem(position)
                viewModel.deleteTask(task)
                Snackbar.make(view, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { viewModel.restoreTask(task) }
                    .setAnchorView(requireActivity().findViewById(R.id.bottomNavigationView))
                    .show()
            }
        }).attachToRecyclerView(recyclerView)

        // Initialize add task button
        addNewTaskButton = view.findViewById(R.id.addNewTaskButton)
        addNewTaskButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, AddTask())
                .addToBackStack(null)
                .commit()
        }

        // Observe tasks
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }

        return view
    }
}
