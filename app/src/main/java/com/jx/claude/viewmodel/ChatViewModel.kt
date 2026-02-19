package com.jx.claude.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jx.claude.api.RetrofitClient
import com.jx.claude.api.StreamingClient
import com.jx.claude.models.*
import com.jx.claude.utils.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PreferencesManager(application)

    private val _chatSessions = MutableLiveData<List<ChatSession>>(emptyList())
    val chatSessions: LiveData<List<ChatSession>> = _chatSessions

    private val _currentSession = MutableLiveData<ChatSession?>()
    val currentSession: LiveData<ChatSession?> = _currentSession

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _availableModels = MutableLiveData<List<ModelInfo>>(FALLBACK_MODELS)
    val availableModels: LiveData<List<ModelInfo>> = _availableModels

    private var streamJob: Job? = null
    private var streamClient: StreamingClient? = null
    private var allSessions: MutableList<ChatSession> = mutableListOf()

    companion object {
        val FALLBACK_MODELS = listOf(
            ModelInfo("claude-sonnet-4-20250514", "Claude Sonnet 4"),
            ModelInfo("claude-opus-4-20250514", "Claude Opus 4"),
            ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku"),
            ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet v2"),
        )
    }

    init {
        allSessions = prefs.loadChatSessions()
        _chatSessions.value = allSessions.toList()
        if (allSessions.isEmpty()) {
            createNewChat()
        } else {
            switchToSession(allSessions.first())
        }
    }

    private fun saveSessions() {
        prefs.saveChatSessions(allSessions)
        _chatSessions.value = allSessions.toList()
    }

    fun createNewChat(): ChatSession {
        val session = ChatSession()
        allSessions.add(0, session)
        saveSessions()
        switchToSession(session)
        return session
    }

    fun switchToSession(session: ChatSession) {
        _currentSession.value = session
        _messages.value = session.messages.toList()
        _error.value = null
    }

    fun deleteSession(session: ChatSession) {
        allSessions.removeAll { it.id == session.id }
        saveSessions()
        if (_currentSession.value?.id == session.id) {
            if (allSessions.isNotEmpty()) {
                switchToSession(allSessions.first())
            } else {
                createNewChat()
            }
        }
    }

    fun fetchModels() {
        val apiKey = prefs.apiKey
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            try {
                val api = RetrofitClient.create(apiKey)
                val response = api.getModels()
                if (response.isSuccessful) {
                    val models = response.body()?.data
                    if (!models.isNullOrEmpty()) {
                        _availableModels.value = models.sortedByDescending { it.createdAt }
                    }
                }
            } catch (_: Exception) {
                // Keep fallback models
            }
        }
    }

    fun sendMessage(userText: String) {
        val apiKey = prefs.apiKey
        if (userText.isBlank() || apiKey.isBlank()) return

        val session = _currentSession.value ?: return

        // Add user message
        val userMsg = ChatMessage(content = userText, isUser = true)
        session.messages.add(userMsg)
        _messages.value = session.messages.toList()
        _isLoading.value = true
        _error.value = null

        // Auto-title from first user message
        if (session.messages.count { it.isUser } == 1) {
            session.title = userText.take(40) + if (userText.length > 40) "…" else ""
            _currentSession.value = session
            saveSessions()
        }

        // Build API messages (skip any empty streaming placeholders)
        val apiMessages = session.messages
            .filter { it.content.isNotBlank() }
            .map { msg ->
                ApiMessage(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.content
                )
            }

        val thinking = if (prefs.thinkingEnabled)
            ThinkingConfig(budgetTokens = prefs.thinkingBudget) else null

        val tools = if (prefs.searchEnabled)
            listOf(Tool(type = "web_search_20250305", name = "web_search", maxUses = 5)) else null

        val maxTokens = if (prefs.thinkingEnabled)
            maxOf(prefs.maxTokens, prefs.thinkingBudget + 1000) else prefs.maxTokens

        val systemPrompt = prefs.systemPrompt.ifBlank { null }

        val request = AnthropicRequest(
            model = prefs.selectedModel,
            maxTokens = maxTokens,
            messages = apiMessages,
            system = systemPrompt,
            thinking = thinking,
            tools = tools
        )

        // Create a placeholder bot message for streaming
        val botMsgId = java.util.UUID.randomUUID().toString()
        val botMsgTimestamp = System.currentTimeMillis()
        session.messages.add(
            ChatMessage(id = botMsgId, content = "", isUser = false, timestamp = botMsgTimestamp)
        )
        _messages.value = session.messages.toList()

        val textBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        streamClient = StreamingClient(apiKey)
        streamJob = viewModelScope.launch {
            try {
                streamClient!!.streamMessage(request)
                    .catch { e ->
                        _error.value = e.message ?: "Stream error"
                        _isLoading.value = false
                    }
                    .collect { event ->
                        when (event.type) {
                            "content_block_start" -> {
                                // Nothing special needed; delta type tells us
                            }
                            "content_block_delta" -> {
                                val delta = event.delta ?: return@collect
                                when (delta.type) {
                                    "thinking_delta" -> {
                                        delta.thinking?.let { thinkingBuilder.append(it) }
                                    }
                                    "text_delta" -> {
                                        delta.text?.let { textBuilder.append(it) }
                                        updateBotMessage(
                                            session, botMsgId, botMsgTimestamp,
                                            textBuilder.toString(),
                                            thinkingBuilder.toString().ifBlank { null }
                                        )
                                    }
                                }
                            }
                            "message_stop" -> {
                                // Finalize
                                updateBotMessage(
                                    session, botMsgId, botMsgTimestamp,
                                    textBuilder.toString().ifBlank { "(empty response)" },
                                    thinkingBuilder.toString().ifBlank { null }
                                )
                                _isLoading.value = false
                                saveSessions()
                            }
                            "error" -> {
                                _error.value = "Stream error"
                                _isLoading.value = false
                            }
                        }
                    }
            } catch (e: Exception) {
                if (textBuilder.isNotEmpty()) {
                    updateBotMessage(
                        session, botMsgId, botMsgTimestamp,
                        textBuilder.toString(),
                        thinkingBuilder.toString().ifBlank { null }
                    )
                    saveSessions()
                } else {
                    // Remove empty bot message
                    session.messages.removeAll { it.id == botMsgId }
                    _messages.value = session.messages.toList()
                }
                if (e !is kotlinx.coroutines.CancellationException) {
                    _error.value = "Error: ${e.localizedMessage}"
                }
                _isLoading.value = false
            }
        }
    }

    private fun updateBotMessage(
        session: ChatSession,
        msgId: String,
        timestamp: Long,
        text: String,
        thinking: String?
    ) {
        val index = session.messages.indexOfFirst { it.id == msgId }
        if (index >= 0) {
            session.messages[index] = ChatMessage(
                id = msgId,
                content = text,
                isUser = false,
                thinkingContent = thinking,
                timestamp = timestamp
            )
            _messages.value = session.messages.toList()
        }
    }

    fun stopStreaming() {
        streamClient?.cancel()
        streamJob?.cancel()
        _isLoading.value = false
        saveSessions()
    }

    fun clearCurrentChat() {
        val session = _currentSession.value ?: return
        session.messages.clear()
        session.title = "New Chat"
        _messages.value = emptyList()
        _currentSession.value = session
        _error.value = null
        saveSessions()
    }

    fun isApiKeySet(): Boolean = prefs.apiKey.isNotBlank()
}