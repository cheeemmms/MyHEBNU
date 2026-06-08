package com.myhebnu.data.remote

import com.myhebnu.data.model.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET

/**
 * GitHub Releases API — used for in-app update checking.
 * Base URL: https://api.github.com/
 */
interface GitHubApi {

    @GET("/repos/cheeemmms/MyHEBNU/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>
}
