package com.jx.claude.utils

object ModelCapabilityHelper {

    data class Capabilities(
        val supportsThinking: Boolean,
        val supportsSearch: Boolean
    )

    fun getCapabilities(modelId: String): Capabilities {
        val id = modelId.lowercase()

        val supportsThinking = id.startsWith("claude-3-7-sonnet") ||
                id.startsWith("claude-sonnet-4") ||
                id.startsWith("claude-opus-4")

        val supportsSearch = id.startsWith("claude-3-5-sonnet") ||
                id.startsWith("claude-3-5-haiku") ||
                id.startsWith("claude-3-7-sonnet") ||
                id.startsWith("claude-sonnet-4") ||
                id.startsWith("claude-opus-4")

        return Capabilities(supportsThinking, supportsSearch)
    }

    /**
     * Approximate cost in USD.
     * Rates (per million tokens): Haiku $1/$5, Sonnet $3/$15, Opus $15/$75
     */
    fun estimateCost(modelId: String, inputTokens: Int, outputTokens: Int): Double {
        val id = modelId.lowercase()
        val (inRate, outRate) = when {
            id.contains("haiku") -> 1.00 to 5.00
            id.contains("opus") -> 15.00 to 75.00
            else -> 3.00 to 15.00 // sonnet variants
        }
        return (inputTokens * inRate + outputTokens * outRate) / 1_000_000.0
    }

    fun formatCost(cost: Double): String = when {
        cost < 0.01 -> "~$${String.format("%.4f", cost)}"
        else -> "~$${String.format("%.2f", cost)}"
    }
}