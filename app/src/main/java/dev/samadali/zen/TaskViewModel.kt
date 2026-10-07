package dev.samadali.zen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = getApplication<ZenApp>().database.taskDao()

    val tasks: LiveData<List<Task>> = dao.getAll()

    fun addTask(name: String, description: String) {
        viewModelScope.launch { dao.insert(Task(name = name, description = description)) }
    }

    fun toggleTaskCompletion(task: Task) {
        val completed = !task.isCompleted
        val updated = task.copy(
            isCompleted = completed,
            completedAt = if (completed) System.currentTimeMillis() else null
        )
        viewModelScope.launch { dao.update(updated) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.delete(task) }
    }

    /** Re-inserts a deleted task with its original id, for undo. */
    fun restoreTask(task: Task) {
        viewModelScope.launch { dao.insert(task) }
    }
}
