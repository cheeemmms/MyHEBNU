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
}
