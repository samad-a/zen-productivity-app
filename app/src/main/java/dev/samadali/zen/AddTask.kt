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
        addTaskButton = view.findViewById(R.id.button7)

        addTaskButton.setOnClickListener {
            val taskName = taskNameInput.text.toString()
            val taskDescription = taskDescriptionInput.text.toString()

            if (taskName.isNotEmpty()) {
                viewModel.addTask(Task(taskName, taskDescription))
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        return view
    }
} 