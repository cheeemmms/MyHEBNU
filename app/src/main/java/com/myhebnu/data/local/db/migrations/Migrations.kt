package com.myhebnu.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE courses ADD COLUMN oddEven INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS exams (
                    id TEXT NOT NULL PRIMARY KEY,
                    courseName TEXT NOT NULL,
                    examDate TEXT NOT NULL,
                    startTime TEXT,
                    endTime TEXT,
                    location TEXT NOT NULL DEFAULT '',
                    campus TEXT NOT NULL DEFAULT '',
                    seatNumber TEXT NOT NULL DEFAULT '',
                    examType TEXT NOT NULL DEFAULT '',
                    department TEXT NOT NULL DEFAULT '',
                    className TEXT NOT NULL DEFAULT '',
                    teacherInfo TEXT NOT NULL DEFAULT '',
                    courseSchedule TEXT NOT NULL DEFAULT '',
                    examMethod TEXT NOT NULL DEFAULT '',
                    semesterYear TEXT NOT NULL DEFAULT '',
                    semesterTerm TEXT NOT NULL DEFAULT '',
                    lastUpdated INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_exams_semesterYear_semesterTerm ON exams (semesterYear, semesterTerm)"
            )
        }
    }
}
