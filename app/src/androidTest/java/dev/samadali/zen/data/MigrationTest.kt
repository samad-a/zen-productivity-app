package dev.samadali.zen.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks that upgrading the app keeps the user's data. Add a test for each new version. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZenDatabase::class.java
    )

    @Test
    fun migrate1To2_keepsTasksAndAddsSessions() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, name, description, isCompleted, createdAt, completedAt) " +
                    "VALUES (1, 'Revise maths', 'Chapter 4', 1, 100, 200)"
            )
        }

        helper.runMigrationsAndValidate(dbName, 2, true).use { db ->
            db.query("SELECT COUNT(*) FROM focus_sessions").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }

        // Open with Room itself, which also checks the schema matches the entities
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ZenDatabase::class.java,
            dbName
        ).build()
        try {
            database.openHelper.readableDatabase.query("SELECT name, completedAt FROM tasks").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Revise maths", cursor.getString(0))
                assertEquals(200L, cursor.getLong(1))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate2To3_givesExistingTasksDefaults() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO tasks (id, name, description, isCompleted, createdAt, completedAt) " +
                    "VALUES (1, 'Gym', '', 0, 100, NULL)"
            )
        }

        helper.runMigrationsAndValidate(dbName, 3, true).use { db ->
            db.query("SELECT category, dueAt, remindAt, position FROM tasks").use { cursor ->
                cursor.moveToFirst()
                assertEquals("OTHER", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
                assertEquals(0, cursor.getInt(3))
            }
        }
    }
}
