package com.myhebnu.di

import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideEASystemApi(retrofit: Retrofit): EASystemApi {
        return retrofit.create(EASystemApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: EASystemApi,
        preferences: UserPreferences
    ): AuthRepository = AuthRepository(api, preferences)

    @Provides
    @Singleton
    fun provideScheduleRepository(
        api: EASystemApi,
        dao: ScheduleDao,
        preferences: UserPreferences
    ): ScheduleRepository = ScheduleRepository(api, dao, preferences)

    @Provides
    @Singleton
    fun provideGradeRepository(
        api: EASystemApi
    ): GradeRepository = GradeRepository(api)

    @Provides
    @Singleton
    fun provideRoomRepository(
        api: EASystemApi
    ): RoomRepository = RoomRepository(api)

    @Provides
    @Singleton
    fun provideExamRepository(
        api: EASystemApi
    ): ExamRepository = ExamRepository(api)
}
