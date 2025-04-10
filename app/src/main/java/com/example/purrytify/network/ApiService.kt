package com.example.purrytify.network

import com.example.purrytify.models.LoginRequest
import com.example.purrytify.models.LoginResponse
import com.example.purrytify.models.RefreshTokenRequest
import com.example.purrytify.models.RefreshTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("/api/verify-token")
    suspend fun verifyToken(): Response<Unit>

    @POST("/api/refresh-token")
    suspend fun refreshToken(@Body refreshTokenRequest: RefreshTokenRequest): Response<RefreshTokenResponse>
}