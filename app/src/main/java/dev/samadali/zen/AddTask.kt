package dev.samadali.zen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText

class AddTask : Fragment() {
    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var taskNameInput: TextInputEditText
    private lateinit var taskDescriptionInput: TextInputEditText
    private lateinit var addTaskButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_addtask, container, false)

        taskNameInput = view.findViewById(R.id.taskNameInput)
        taskDescriptionInput = view.findViewById(R.id.taskDescriptionInput)
        addTaskButton = view.findViewById(R.id.addTaskButton)

        addTaskButton.setOnClickListener {
            val taskName = taskNameInput.text.toString().trim()
            val taskDescription = taskDescriptionInput.text.toString().trim()

            if (taskName.isNotEmpty()) {
                viewModel.addTask(taskName, taskDescription)
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                taskNameInput.error = getString(R.string.error_task_name_required)
            }
        }

        return view
    }
} 