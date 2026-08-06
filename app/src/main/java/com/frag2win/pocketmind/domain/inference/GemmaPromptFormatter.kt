package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.local.ChatMessage

object GemmaPromptFormatter {
    
    /**
     * Formats an entire conversation history into Gemma's required instruction-tuned syntax.
     * Includes character-based truncation to avoid exceeding token limits (4096 tokens).
     * Smart Anchoring: If a PDF was attached, we preserve context from that point forward
     * to allow follow-up questions about the document.
     */
    fun formatHistory(messages: List<ChatMessage>, maxChars: Int = 12000): String {
        val builder = StringBuilder()
        
        // Find the index of the most recent PDF attachment
        val lastPdfIndex = messages.indexOfLast { 
            it.displayContent?.startsWith("__PDF_ATTACHED_FILE__:", ignoreCase = true) == true ||
            it.content.contains("TEXT FROM PDF:", ignoreCase = true)
        }

        // If a PDF is in the history, we only care about the conversation 
        // from that document onwards to prevent "context contamination" from 
        // unrelated previous topics.
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
            if (currentChars + msg.content.length > maxChars) break
            finalMessages.add(0, msg)
            currentChars += msg.content.length
        }
        
        for (message in finalMessages) {
            val roleTag = if (message.role == "user") "user" else "model"
            builder.append("<start_of_turn>${roleTag}\n${message.content.trim()}<end_of_turn>\n")
        }
        
        // Always append the final model start token to cue the AI to generate
        builder.append("<start_of_turn>model\n")
        
        return builder.toString()
    }

    /**
     * Formats a specific prompt to generate a short summary title for a chat session.
     */
    fun formatSummaryPrompt(userMessage: String, assistantResponse: String): String {
        val prompt = "Summarize the following exchange into a 3-5 word title. " +
                "Do not use quotation marks or leading/trailing spaces. " +
                "The title should be concise and descriptive.\n\n" +
                "User: $userMessage\n" +
                "Assistant: $assistantResponse\n\n" +
                "Title:"
        return "<start_of_turn>user\n${prompt}<end_of_turn>\n<start_of_turn>model\n"
    }
}
