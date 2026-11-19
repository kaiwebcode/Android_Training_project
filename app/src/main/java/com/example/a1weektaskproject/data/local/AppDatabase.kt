package com.example.a1weektaskproject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database definition for task persistence.
 */
@Database(
    entities = [TaskEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN origin TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        // v2/v3 assigned GOREST from the API, but defaulted other legacy rows to LOCAL.
        // v4/v5 could rewrite either label based on sync state, so neither label is reliable.
        internal val MIGRATIONS = arrayOf(
            MIGRATION_1_6,
            quarantineLegacyOrigins(2, preserveGoRest = true),
            quarantineLegacyOrigins(3, preserveGoRest = true),
            quarantineLegacyOrigins(4, preserveGoRest = false),
            quarantineLegacyOrigins(5, preserveGoRest = false)
        )

        private fun quarantineLegacyOrigins(fromVersion: Int, preserveGoRest: Boolean) =
            object : Migration(fromVersion, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Some historical paths created a different origin default. Rebuild to the v6 schema.
                    db.execSQL(
                        """
                        CREATE TABLE tasks_v6 (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            completed INTEGER NOT NULL,
                            priority TEXT NOT NULL,
                            dueDate TEXT NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            syncState TEXT NOT NULL,
                            syncOp TEXT NOT NULL,
                            origin TEXT NOT NULL DEFAULT 'UNKNOWN'
                        )
                        """.trimIndent()
                    )
                    val origin = if (preserveGoRest) {
                        "CASE WHEN origin = 'GOREST' THEN 'GOREST' ELSE 'UNKNOWN' END"
                    } else {
                        "'UNKNOWN'"
                    }
                    db.execSQL(
                        """
                        INSERT INTO tasks_v6
                            (id, title, description, completed, priority, dueDate, updatedAt, syncState, syncOp, origin)
                        SELECT id, title, description, completed, priority, dueDate, updatedAt, syncState, syncOp, $origin
                        FROM tasks
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE tasks")
                    db.execSQL("ALTER TABLE tasks_v6 RENAME TO tasks")
                }
            }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database.db"
                )
                .addMigrations(*MIGRATIONS)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
