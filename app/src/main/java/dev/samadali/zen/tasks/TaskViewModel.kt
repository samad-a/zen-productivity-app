package dev.samadali.zen.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.samadali.zen.ZenApp
import dev.samadali.zen.data.Task
import dev.samadali.zen.data.TaskCategory
import dev.samadali.zen.tasks.reminders.TaskReminders
import kotlinx.coroutines.launch

/** A row in the task list: a task, or the header of the finished tasks section. */
sealed interface TaskListItem {
    data class TaskRow(val task: Task) : TaskListItem
    data class CompletedHeader(val count: Int, val expanded: Boolean) : TaskListItem
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<ZenApp>()
    private val dao = app.database.taskDao()

    val tasks: LiveData<List<Task>> = dao.getAll()

    private val showCompleted = MutableLiveData(false)

    /** Unfinished tasks, then a collapsible section of finished ones. */
    val items: LiveData<List<TaskListItem>> = MediatorLiveData<List<TaskListItem>>().apply {
        fun update() {
            val all = tasks.value ?: return
            value = buildItems(all, showCompleted.value == true)
        }
        addSource(tasks) { update() }
        addSource(showCompleted) { update() }
    }

    fun toggleCompletedSection() {
        showCompleted.value = showCompleted.value != true
    }

    suspend fun getTask(id: Long): Task? = dao.getById(id)

    fun addTask(name: String, description: String, category: TaskCategory, dueAt: Long?, remindAt: Long?) {
        viewModelScope.launch {
            val task = Task(
                name = name,
                description = description,
                category = category,
                dueAt = dueAt,
                remindAt = remindAt,
                position = dao.nextPosition()
            )
            val id = dao.insert(task)
            TaskReminders.sync(app, task.copy(id = id))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.update(task)
            TaskReminders.sync(app, task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        val completed = !task.isCompleted
        updateTask(
            task.copy(
                isCompleted = completed,
                completedAt = if (completed) System.currentTimeMillis() else null
            )
        )
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
            TaskReminders.cancel(app, task.id)
        }
    }

    /** Re-inserts deleted tasks with their original ids, for undo. */
    fun restoreTasks(tasks: List<Task>) {
        viewModelScope.launch {
            dao.insertAll(tasks)
            tasks.forEach { TaskReminders.sync(app, it) }
        }
    }

    /** Deletes every finished task and returns them, so the deletion can be undone. */
    fun clearCompleted(): List<Task> {
        val completed = tasks.value.orEmpty().filter { it.isCompleted }
        viewModelScope.launch { dao.deleteAll(completed) }
        return completed
    }

    /** Saves a new order for the unfinished tasks after a drag. */
    fun reorder(orderedTasks: List<Task>) {
        val updated = orderedTasks.mapIndexedNotNull { index, task ->
            if (task.position == index) null else task.copy(position = index)
        }
        if (updated.isNotEmpty()) viewModelScope.launch { dao.updateAll(updated) }
    }

    companion object {
        fun buildItems(tasks: List<Task>, showCompleted: Boolean): List<TaskListItem> {
            val (done, todo) = tasks.partition { it.isCompleted }
            return buildList {
                todo.forEach { add(TaskListItem.TaskRow(it)) }
                if (done.isNotEmpty()) {
                    add(TaskListItem.CompletedHeader(done.size, showCompleted))
                    if (showCompleted) done.forEach { add(TaskListItem.TaskRow(it)) }
                }
            }
        }
    }
}
