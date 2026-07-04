package com.frag2win.pocketmind.data.local

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = ChatMessage::class)
@Entity(tableName = "chat_messages_fts")
data class ChatMessageSearchEntity(
    val content: String,
    val role: String
)
