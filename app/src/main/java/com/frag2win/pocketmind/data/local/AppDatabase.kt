package com.frag2win.pocketmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatMessage::class, ChatSession::class, ChatMessageSearchEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
