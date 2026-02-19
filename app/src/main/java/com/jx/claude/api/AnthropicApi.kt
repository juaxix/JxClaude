package com.jx.claude.api

import com.jx.claude.models.AnthropicRequest
import com.jx.claude.models.AnthropicResponse
import com.jx.claude.models.ModelsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface AnthropicApi {

    @Headers("Content-Type: application/json")
    @POST("v1/messages")
    suspend fun sendMessage(@Body request: AnthropicRequest): Response<AnthropicResponse>

    @GET("v1/models")
    suspend fun getModels(): Response<ModelsResponse>
}