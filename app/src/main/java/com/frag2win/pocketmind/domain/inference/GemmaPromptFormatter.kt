package com.frag2win.pocketmind.domain.inference

import com.frag2win.pocketmind.data.local.ChatMessage

object GemmaPromptFormatter {
    
    /**
     * Formats an entire conversation history into Gemma's required instruction-tuned syntax.
     */
    fun formatHistory(messages: List<ChatMessage>): String {
        val builder = StringBuilder()
        
        for (message in messages) {
            val roleTag = if (message.role == "user") "user" else "model"
            builder.append("<start_of_turn>${roleTag}\n${message.content.trim()}<end_of_turn>\n")
        }
        
        // Always append the final model start token to cue the AI to generate
        builder.append("<start_of_turn>model\n")
        
        return builder.toString()
    }
}
