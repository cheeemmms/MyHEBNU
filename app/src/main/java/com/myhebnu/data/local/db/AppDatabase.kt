package com.myhebnu.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.db.entity.CourseEntity

@Database(
    entities = [CourseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}
