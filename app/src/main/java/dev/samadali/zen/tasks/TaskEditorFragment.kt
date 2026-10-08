package dev.samadali.zen.tasks

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dev.samadali.zen.R
import dev.samadali.zen.databinding.FragmentAddTaskBinding

class AddTaskFragment : Fragment(R.layout.fragment_add_task) {
    private val viewModel: TaskViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAddTaskBinding.bind(view)

        binding.addTaskButton.setOnClickListener {
            val taskName = binding.taskNameInput.text?.toString().orEmpty().trim()
            val taskDescription = binding.taskDescriptionInput.text?.toString().orEmpty().trim()

            if (taskName.isNotEmpty()) {
                viewModel.addTask(taskName, taskDescription)
                parentFragmentManager.popBackStack()
            } else {
                binding.taskNameInput.error = getString(R.string.error_task_name_required)
            }
        }
    }
}
