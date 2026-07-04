package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.local.ChatMessage

object GemmaPromptFormatter {
    
    /**
     * Formats an entire conversation history into Gemma's required instruction-tuned syntax.
     * Includes character-based truncation to avoid exceeding token limits (4096 tokens).
     */
    fun formatHistory(messages: List<ChatMessage>, maxChars: Int = 12000): String {
        val builder = StringBuilder()
        
        // Iterate backwards and stop when we exceed character limit (~3000 tokens)
        // This leaves room for the model to generate a response.
        val relevantMessages = mutableListOf<ChatMessage>()
        var currentChars = 0
        for (i in messages.indices.reversed()) {
            val msg = messages[i]
            // We check against content length, adding a small buffer for the turn tags
            if (currentChars + msg.content.length > maxChars) break
            relevantMessages.add(0, msg)
            currentChars += msg.content.length
        }
        
        for (message in relevantMessages) {
            val roleTag = if (message.role == "user") "user" else "model"
            builder.append("<start_of_turn>${roleTag}\n${message.content.trim()}<end_of_turn>\n")
        }
        
        // Always append the final model start token to cue the AI to generate
        builder.append("<start_of_turn>model\n")
        
        return builder.toString()
    }
}
