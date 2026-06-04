package com.myhebnu.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET

/**
 * CAS (Central Authentication Service) API for SSO login.
 * Base URL: http://cas.hebtu.edu.cn/
 */
interface CasApi {

    /**
     * Get the RSA public key used for password encryption before login.
     * Returns: `{ "modulus": "...hex...", "exponent": "...hex..." }`
     */
    @GET("/cas/v2/getPubKey")
    suspend fun getPublicKey(): Response<JsonObject>
}
