package com.jx.claude.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

// ── Chat Session ──
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Chat",
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

// ── Local UI Model ──
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val thinkingContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentNames: List<String>? = null,
    // Token / performance metrics
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val tokensPerSecond: Float? = null,
    val estimatedCost: Double? = null
)

// ── Anthropic API Request ──
data class AnthropicRequest(
    val model: String = "claude-sonnet-4-20250514",
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val messages: List<ApiMessage>,
    val stream: Boolean = false,
    val system: String? = null,
    val thinking: ThinkingConfig? = null,
    val tools: List<Tool>? = null,
    val temperature: Double? = null
)

data class ThinkingConfig(
    val type: String = "enabled",
    @SerializedName("budget_tokens") val budgetTokens: Int = 10000
)

data class Tool(
    val type: String,
    val name: String,
    @SerializedName("max_uses") val maxUses: Int? = null
)

data class ApiMessage(
    val role: String,
    val content: Any // String for text-only, List<ContentBlock> for multimodal
)

// ── Anthropic API Response ──
data class AnthropicResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<ContentBlock>? = null,
    val model: String? = null,
    @SerializedName("stop_reason") val stopReason: String? = null,
    val usage: Usage? = null,
    val error: ErrorBody? = null
)

data class Usage(
    @SerializedName("input_tokens") val inputTokens: Int = 0,
    @SerializedName("output_tokens") val outputTokens: Int = 0
)

data class ErrorBody(
    val type: String? = null,
    val message: String? = null
)

// ── Models List ──
data class ModelsResponse(
    val data: List<ModelInfo>? = null,
    @SerializedName("has_more") val hasMore: Boolean? = null
)

data class ModelInfo(
    val id: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ── Streaming Events ──
data class StreamEvent(
    val type: String = "",
    val message: AnthropicResponse? = null,
    val index: Int? = null,
    @SerializedName("content_block") val contentBlock: ContentBlock? = null,
    val delta: Delta? = null,
    val usage: Usage? = null
)

data class Delta(
    val type: String? = null,
    val text: String? = null,
    val thinking: String? = null,
    @SerializedName("stop_reason") val stopReason: String? = null
)