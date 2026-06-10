package com.myhebnu.di

import android.content.Context
import androidx.room.Room
import com.myhebnu.data.local.db.AppDatabase
import com.myhebnu.data.local.db.dao.ExamDao
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.db.migrations.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "myhebnu.db"
        ).addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
          .fallbackToDestructiveMigration(false).build()
    }

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    fun provideExamDao(database: AppDatabase): ExamDao {
        return database.examDao()
    }
}
