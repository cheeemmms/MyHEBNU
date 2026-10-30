package com.myhebnu.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myhebnu.data.local.db.dao.ExamDao
import com.myhebnu.data.local.db.dao.GradeDao
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.db.dao.WishlistDao
import com.myhebnu.data.local.db.entity.CourseEntity
import com.myhebnu.data.local.db.entity.ExamEntity
import com.myhebnu.data.local.db.entity.GradeEntity
import com.myhebnu.data.local.db.entity.WishlistEntity

@Database(
    entities = [CourseEntity::class, ExamEntity::class, GradeEntity::class, WishlistEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun examDao(): ExamDao
    abstract fun gradeDao(): GradeDao
    abstract fun wishlistDao(): WishlistDao
}
