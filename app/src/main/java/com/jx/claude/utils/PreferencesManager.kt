package com.jx.claude.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jx.claude.models.ChatSession

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("claude_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) {
            prefs.edit().putString("api_key", value).apply()
        }

    var selectedModel: String
        get() = prefs.getString("selected_model", "claude-sonnet-4-20250514")
            ?: "claude-sonnet-4-20250514"
        set(value) {
            prefs.edit().putString("selected_model", value).apply()
        }

    var thinkingEnabled: Boolean
        get() = prefs.getBoolean("thinking_enabled", false)
        set(value) {
            prefs.edit().putBoolean("thinking_enabled", value).apply()
        }

    var thinkingBudget: Int
        get() = prefs.getInt("thinking_budget", 10000)
        set(value) {
            prefs.edit().putInt("thinking_budget", value).apply()
        }

    var searchEnabled: Boolean
        get() = prefs.getBoolean("search_enabled", false)
        set(value) {
            prefs.edit().putBoolean("search_enabled", value).apply()
        }

    var maxTokens: Int
        get() = prefs.getInt("max_tokens", 4096)
        set(value) {
            prefs.edit().putInt("max_tokens", value).apply()
        }

    var systemPrompt: String
        get() = prefs.getString("system_prompt", "") ?: ""
        set(value) {
            prefs.edit().putString("system_prompt", value).apply()
        }

    fun saveChatSessions(sessions: List<ChatSession>) {
        prefs.edit().putString("chat_sessions", gson.toJson(sessions)).apply()
    }

    fun loadChatSessions(): MutableList<ChatSession> {
        val json = prefs.getString("chat_sessions", null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<ChatSession>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }
}