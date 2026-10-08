package dev.samadali.zen.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: ZenDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ZenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_assignsDistinctIdsToTasksWithTheSameName() = runBlocking {
        val first = dao.insert(Task(name = "Gym", description = ""))
        val second = dao.insert(Task(name = "Gym", description = ""))

        assertTrue(first != second)
        assertEquals(2, dao.getAll().awaitValue().size)
    }

    @Test
    fun getAll_listsIncompleteTasksFirstInCreationOrder() = runBlocking {
        dao.insert(Task(name = "done", description = "", isCompleted = true, createdAt = 1))
        dao.insert(Task(name = "second", description = "", createdAt = 3))
        dao.insert(Task(name = "first", description = "", createdAt = 2))

        val names = dao.getAll().awaitValue().map { it.name }
        assertEquals(listOf("first", "second", "done"), names)
    }

    @Test
    fun update_changesOnlyTheMatchingTask() = runBlocking {
        val id = dao.insert(Task(name = "Gym", description = ""))
        dao.insert(Task(name = "Gym", description = ""))

        val task = dao.getAll().awaitValue().first { it.id == id }
        dao.update(task.copy(isCompleted = true, completedAt = 100))

        val tasks = dao.getAll().awaitValue()
        assertEquals(1, tasks.count { it.isCompleted })
        assertEquals(100L, tasks.first { it.id == id }.completedAt)
    }

    @Test
    fun deleteThenInsert_restoresTheTaskWithItsId() = runBlocking {
        val id = dao.insert(Task(name = "Revise", description = "chapter 3"))
        val task = dao.getAll().awaitValue().single()

        dao.delete(task)
        assertTrue(dao.getAll().awaitValue().isEmpty())

        dao.insert(task)
        assertEquals(listOf(task), dao.getAll().awaitValue())
        assertEquals(id, task.id)
    }

    @Test
    fun getAll_ordersUnfinishedByPositionAndFinishedByMostRecent() = runBlocking {
        dao.insert(Task(name = "b", description = "", position = 1))
        dao.insert(Task(name = "a", description = "", position = 0))
        dao.insert(Task(name = "older done", description = "", isCompleted = true, completedAt = 10))
        dao.insert(Task(name = "newer done", description = "", isCompleted = true, completedAt = 20))

        val names = dao.getAll().awaitValue().map { it.name }
        assertEquals(listOf("a", "b", "newer done", "older done"), names)
    }

    @Test
    fun nextPosition_isAfterTheLastUnfinishedTask() = runBlocking {
        assertEquals(0, dao.nextPosition())
        dao.insert(Task(name = "a", description = "", position = 4))
        dao.insert(Task(name = "done", description = "", isCompleted = true, position = 9))

        assertEquals(5, dao.nextPosition())
    }

    @Test
    fun getUpcomingReminders_skipsPastAndFinishedTasks() = runBlocking {
        dao.insert(Task(name = "soon", description = "", remindAt = 200))
        dao.insert(Task(name = "past", description = "", remindAt = 50))
        dao.insert(Task(name = "done", description = "", remindAt = 300, isCompleted = true))
        dao.insert(Task(name = "none", description = ""))

        assertEquals(listOf("soon"), dao.getUpcomingReminders(now = 100).map { it.name })
    }

    /** Waits for the LiveData's next value; Room delivers query results asynchronously. */
    private fun <T> LiveData<T>.awaitValue(): T {
        var result: T? = null
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(value: T) {
                result = value
                latch.countDown()
                removeObserver(this)
            }
        }
        observeForever(observer)
        check(latch.await(2, TimeUnit.SECONDS)) { "LiveData value was never set" }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
