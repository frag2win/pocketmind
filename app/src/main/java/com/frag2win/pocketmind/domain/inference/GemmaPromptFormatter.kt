package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.local.ChatMessage

object GemmaPromptFormatter {
    
    /**
     * Cleans raw model output by stripping echo tags like <start_of_turn> and <end_of_turn>.
     * CRITICAL: Does NOT call .trim() so spaces between streaming tokens are preserved!
     */
    fun sanitizeOutput(text: String): String {
        return text
            .replace("<start_of_turn>model", "")
            .replace("<start_of_turn>user", "")
            .replace("<start_of_turn>", "")
            .replace("<end_of_turn>", "")
            .replace("[System Context:", "")
            .replace("System Context:", "")
            .replace("[System Instruction:", "")
    }

    /**
     * Formats an entire conversation history into Gemma's required instruction-tuned syntax.
     * Includes character-based truncation to avoid exceeding token limits (4096 tokens).
     * Smart Anchoring: If a PDF was attached, we preserve context from that point forward
     * to allow follow-up questions about the document.
     */
    fun formatHistory(
        messages: List<ChatMessage>,
        displayName: String = "User",
        maxChars: Int = 12000
    ): String {
        val builder = StringBuilder()
        val effectiveName = if (displayName.isBlank()) "User" else displayName
        val systemPreamble = "You are PocketMind, an offline-first AI assistant running locally on Android. You are talking to $effectiveName."

        // Find the index of the most recent PDF attachment
        val lastPdfIndex = messages.indexOfLast { 
            it.displayContent?.startsWith("__PDF_ATTACHED_FILE__:", ignoreCase = true) == true ||
            it.content.contains("TEXT FROM PDF:", ignoreCase = true)
        }

        val relevantMessages = if (lastPdfIndex != -1) {
            messages.subList(lastPdfIndex, messages.size)
        } else {
            messages
        }

        // Iterate backwards and stop when we exceed character limit (~3000 tokens)
        val finalMessages = mutableListOf<ChatMessage>()
        var currentChars = 0
        for (i in relevantMessages.indices.reversed()) {
            val msg = relevantMessages[i]
            val cleanContent = sanitizeOutput(msg.content).trim()
            if (cleanContent.isBlank()) continue
            if (currentChars + cleanContent.length > maxChars) break
            finalMessages.add(0, msg)
            currentChars += cleanContent.length
        }

        if (finalMessages.isEmpty()) {
            builder.append("<start_of_turn>user\n$systemPreamble\nHello!<end_of_turn>\n<start_of_turn>model\n")
            return builder.toString()
        }

        // Build Gemma instruction-tuned turn history
        for (index in finalMessages.indices) {
            val message = finalMessages[index]
            val cleanContent = sanitizeOutput(message.content).trim()
            val role = if (message.role == "user") "user" else "model"

            builder.append("<start_of_turn>$role\n")
            if (index == 0 && message.role == "user") {
                builder.append("$systemPreamble\n\n")
            }
            builder.append("$cleanContent<end_of_turn>\n")
        }

        // Cue model for completion
        builder.append("<start_of_turn>model\n")
        return builder.toString()
    }

    /**
     * Formats a specific prompt to generate a short summary title for a chat session.
     */
    fun formatSummaryPrompt(userMessage: String, assistantResponse: String): String {
        val cleanUser = sanitizeOutput(userMessage).trim()
        val cleanAssistant = sanitizeOutput(assistantResponse).trim()
        return "<start_of_turn>user\nSummarize the following exchange into a 3-5 word title. " +
                "Do not use quotation marks or leading/trailing spaces. " +
                "The title should be concise and descriptive.\n\n" +
                "User: $cleanUser\n" +
                "Assistant: $cleanAssistant\n\n" +
                "Title:<end_of_turn>\n<start_of_turn>model\n"
    }
}
