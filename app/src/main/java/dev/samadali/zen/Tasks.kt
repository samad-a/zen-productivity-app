package dev.samadali.zen

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        
        // Create a custom layout manager that centers items
        val layoutManager = object : LinearLayoutManager(context) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams): Boolean {
                val width = recyclerView.width
                if (width > 0) {
                    lp.width = (width * 0.9).toInt() // Make items take up 90% of the width
                }
                return true
            }
        }.apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = taskAdapter

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