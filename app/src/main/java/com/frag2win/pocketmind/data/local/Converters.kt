package com.frag2win.pocketmind.data.local

import androidx.room.TypeConverter
import com.frag2win.pocketmind.data.repository.SearchResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSearchResultList(value: List<SearchResult>?): String? {
        if (value == null) return null
        val type = object : TypeToken<List<SearchResult>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toSearchResultList(value: String?): List<SearchResult>? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<List<SearchResult>>() {}.type
        return try {
            gson.fromJson(value, type)
        } catch (_: Exception) {
            null
        }
    }
}
