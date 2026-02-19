package com.jx.claude.api

import com.google.gson.Gson
import com.jx.claude.models.AnthropicRequest
import com.jx.claude.models.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class StreamingClient(private val apiKey: String) {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var currentCall: Call? = null

    fun cancel() {
        currentCall?.cancel()
        currentCall = null
    }

    fun streamMessage(request: AnthropicRequest): Flow<StreamEvent> = flow {
        val streamRequest = request.copy(stream = true)
        val jsonBody = gson.toJson(streamRequest)

        val httpRequest = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(httpRequest)
        currentCall = call

        val response = try {
            call.execute()
        } catch (e: java.io.IOException) {
            if (call.isCanceled()) return@flow
            throw e
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            response.close()
            throw Exception("API Error ${response.code}: $errorBody")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (call.isCanceled()) break
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val event = gson.fromJson(data, StreamEvent::class.java)
                        if (event != null) emit(event)
                    } catch (_: Exception) {
                        // Skip malformed JSON
                    }
                }
            }
        } finally {
            reader.close()
            response.close()
            currentCall = null
        }
    }.flowOn(Dispatchers.IO)
}