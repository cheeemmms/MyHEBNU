package com.myhebnu.di

import android.content.Context
import com.myhebnu.data.local.db.dao.ScheduleDao
import com.myhebnu.data.local.preferences.CredentialManager
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.remote.CasApi
import com.myhebnu.data.remote.EASystemApi
import com.myhebnu.data.remote.GitHubApi
import com.myhebnu.data.remote.PersistentCookieJar
import com.myhebnu.data.remote.SessionManager
import com.myhebnu.data.remote.interceptor.AuthInterceptor
import com.myhebnu.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideCasApi(@javax.inject.Named("cas") retrofit: Retrofit): CasApi {
        return retrofit.create(CasApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        cookieJar: PersistentCookieJar,
        sessionManager: SessionManager,
        preferences: UserPreferences,
        authInterceptor: AuthInterceptor,
        credentialManager: CredentialManager,
        okHttpClient: okhttp3.OkHttpClient
    ): AuthRepository = AuthRepository(cookieJar, sessionManager, preferences, authInterceptor, credentialManager, okHttpClient)

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

    @Provides
    @Singleton
    fun provideGitHubApi(@javax.inject.Named("github") retrofit: Retrofit): GitHubApi {
        return retrofit.create(GitHubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(
        githubApi: GitHubApi,
        preferences: UserPreferences,
        @ApplicationContext context: Context
    ): UpdateRepository = UpdateRepository(githubApi, preferences, context)
}
