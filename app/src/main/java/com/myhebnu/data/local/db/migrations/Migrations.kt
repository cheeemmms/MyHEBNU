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

    val MIGRATION_3_4 = object : Migration(3, 4) {        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS grades (
                    id TEXT NOT NULL PRIMARY KEY,
                    courseName TEXT NOT NULL,
                    courseCode TEXT NOT NULL DEFAULT '',
                    credit REAL NOT NULL DEFAULT 0,
                    score TEXT NOT NULL DEFAULT '',
                    scoreValue REAL,
                    classId TEXT NOT NULL DEFAULT '',
                    teachingClassName TEXT NOT NULL DEFAULT '',
                    department TEXT NOT NULL DEFAULT '',
                    semesterYear TEXT NOT NULL DEFAULT '',
                    semesterTerm TEXT NOT NULL DEFAULT '',
                    semesterName TEXT NOT NULL DEFAULT '',
                    lastUpdated INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_grades_semesterYear_semesterTerm ON grades (semesterYear, semesterTerm)"
            )
        }
    }

    /**
     * 课程表新增周次/节次位掩码（对应教务返回的 oldzc / oldjc）。
     *
     * 老数据一律写 0，表示"尚未采集掩码"——判定时回落 startWeek..endWeek + oddEven，
     * 下次刷新课表即写入真实掩码，届时段级单双、多段周次、"第 N 周"等写法全部精确。
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE courses ADD COLUMN weekBits INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE courses ADD COLUMN periodBits INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 新增「想看」表。
     *
     * 只建新表、不动任何既有表——课表/成绩/考试数据零风险。
     * 字段类型与默认值必须和 [com.myhebnu.data.local.db.entity.WishlistEntity] 完全一致，
     * 否则 Room 启动时的 schema 校验会直接抛错（这是好事：宁可报错也不静默错位）。
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS wishlist (
                    bibId TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    publisher TEXT NOT NULL,
                    pubYear TEXT NOT NULL,
                    isbn TEXT NOT NULL,
                    coverUrl TEXT NOT NULL,
                    addedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}
