package com.myhebnu.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myhebnu.data.local.db.dao.ExamDao
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.local.db.entity.ExamEntity

@Database(
    entities = [CourseEntity::class, ExamEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun examDao(): ExamDao
}
