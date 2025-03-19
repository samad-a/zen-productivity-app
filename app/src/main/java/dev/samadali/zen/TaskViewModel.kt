package dev.samadali.zen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TaskViewModel : ViewModel() {
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val taskList = mutableListOf<Task>()

    init {
        _tasks.value = taskList
    }

    fun addTask(task: Task) {
        taskList.add(task)
        _tasks.value = taskList.toList()
    }

    fun toggleTaskCompletion(task: Task) {
        val index = taskList.indexOfFirst { it.name == task.name }
        if (index != -1) {
            taskList[index] = task.copy(isCompleted = !task.isCompleted)
            _tasks.value = taskList.toList()
        }
    }
} 