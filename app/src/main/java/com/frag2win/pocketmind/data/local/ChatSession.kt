package com.frag2win.pocketmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "New Chat",
    val timestamp: Long = System.currentTimeMillis()
)
