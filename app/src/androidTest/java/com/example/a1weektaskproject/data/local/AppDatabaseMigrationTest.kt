package com.example.a1weektaskproject.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @Test fun upgradesPopulatedVersion1() = verifyUpgrade(1)
    @Test fun upgradesPopulatedVersion2() = verifyUpgrade(2)
    @Test fun upgradesPopulatedVersion3() = verifyUpgrade(3)

    @Test
    fun upgradesPopulatedVersion4() {
        verifyUpgrade(4)
        verifyUpgrade(4, legacyGoRestDefault = true)
    }

    @Test
    fun upgradesPopulatedVersion5() {
        verifyUpgrade(5)
        verifyUpgrade(5, legacyGoRestDefault = true)
    }

    private fun verifyUpgrade(fromVersion: Int, legacyGoRestDefault: Boolean = false) = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "day13-migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        val legacySchema = instrumentation.context.assets
            .open("${AppDatabase::class.java.name}/$fromVersion.json")
            .bufferedReader().use { JSONObject(it.readText()) }
            .getJSONObject("database").getJSONArray("entities").getJSONObject(0)
            .getString("createSql").replace("\${TABLE_NAME}", "tasks")
        val createSql = if (legacyGoRestDefault) {
            legacySchema.replace("DEFAULT 'LOCAL'", "DEFAULT 'GOREST'")
        } else {
            legacySchema
        }

        val localSynced = TaskEntity(
            id = "local-synced",
            title = "Completed legacy local task",
            description = "Keep this description",
            completed = true,
            priority = "HIGH",
            dueDate = "Next week",
            updatedAt = 123,
            origin = if (fromVersion == 4 || fromVersion == 5) TaskOrigin.GOREST else TaskOrigin.LOCAL
        )
        val localEdit = localSynced.copy(
            id = "local-edit",
            title = "Pending legacy local edit",
            completed = false,
            updatedAt = 124,
            syncState = SyncState.PENDING,
            syncOp = SyncOp.UPDATE,
            origin = if (fromVersion == 4) TaskOrigin.GOREST else TaskOrigin.LOCAL
        )
        val goRestEdit = localEdit.copy(
            id = "confirmed-gorest-id",
            title = "Pending GoRest edit",
            completed = true,
            updatedAt = 125,
            origin = if (fromVersion == 5) TaskOrigin.LOCAL else TaskOrigin.GOREST
        )
        val interrupted = localEdit.copy(
            id = "legacy-interrupted",
            syncState = SyncState.SYNCING,
            syncOp = SyncOp.CREATE,
            origin = TaskOrigin.LOCAL
        )
        val legacyTasks = listOf(localSynced, localEdit, goRestEdit, interrupted)

        SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
            legacy.execSQL(createSql)
            for (task in legacyTasks) {
                val values = arrayOf<Any>(
                    task.id, task.title, task.description, if (task.completed) 1 else 0, task.priority,
                    task.dueDate, task.updatedAt, task.syncState, task.syncOp
                )
                if (fromVersion == 1) {
                    legacy.execSQL("INSERT INTO tasks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", values)
                } else {
                    legacy.execSQL("INSERT INTO tasks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values + task.origin)
                }
            }
            legacy.version = fromVersion
        }

        // Opening with Room exercises its generated migration validation against the populated file.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
        try {
                val dao = db.taskDao()
                val migrated = legacyTasks.map { task ->
                    val verifiedGoRest = fromVersion in 2..3 && task.origin == TaskOrigin.GOREST
                    task.copy(origin = if (verifiedGoRest) TaskOrigin.GOREST else TaskOrigin.UNKNOWN)
                }
                assertEquals(migrated.associateBy { it.id }, dao.getAllTasks().associateBy { it.id })
                assertEquals(0, dao.resetSyncingToPending())
                assertEquals(emptyList<TaskEntity>(), dao.getPendingSyncTasks())
                assertEquals(0, dao.markTasksSyncing(legacyTasks.map { it.id }))

                dao.cacheGoRestTask(goRestEdit.copy(title = "Remote snapshot", completed = false, origin = TaskOrigin.GOREST))
                assertEquals(goRestEdit.copy(origin = TaskOrigin.GOREST), dao.getTaskById(goRestEdit.id))

                val verified = localEdit.copy(id = "new-verified-local", origin = TaskOrigin.LOCAL)
                dao.insertTask(verified)
                dao.confirmGoRestOrigin(verified.id)
                assertEquals(listOf(verified), dao.getPendingSyncTasks())
        } finally {
            db.close()
        }
    }
}
