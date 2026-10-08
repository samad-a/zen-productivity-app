package dev.samadali.zen.settings

import dev.samadali.zen.data.FocusSession
import dev.samadali.zen.data.Task
import org.json.JSONArray
import org.json.JSONObject

/** Everything the user has created, as JSON they can keep or move elsewhere. */
object DataExport {
    const val VERSION = 1

    fun toJson(tasks: List<Task>, sessions: List<FocusSession>, exportedAt: Long): String {
        val root = JSONObject()
            .put("app", "Zen")
            .put("formatVersion", VERSION)
            .put("exportedAt", exportedAt)
            .put("tasks", JSONArray(tasks.map { task ->
                JSONObject()
                    .put("name", task.name)
                    .put("description", task.description)
                    .put("category", task.category.name)
                    .put("completed", task.isCompleted)
                    .put("createdAt", task.createdAt)
                    .put("completedAt", task.completedAt ?: JSONObject.NULL)
                    .put("dueAt", task.dueAt ?: JSONObject.NULL)
                    .put("remindAt", task.remindAt ?: JSONObject.NULL)
            }))
            .put("focusSessions", JSONArray(sessions.map { session ->
                JSONObject()
                    .put("startedAt", session.startedAt)
                    .put("completedAt", session.completedAt)
                    .put("durationMinutes", session.durationMillis / 60_000)
            }))
        return root.toString(2)
    }
}
