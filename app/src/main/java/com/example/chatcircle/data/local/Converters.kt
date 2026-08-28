package com.example.chatcircle.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromMemberIds(memberIds: List<String>): String {
        return JSONArray(memberIds).toString()
    }

    @TypeConverter
    fun toMemberIds(json: String): List<String> {
        val array = JSONArray(json)
        return List(array.length()) { i -> array.getString(i) }
    }

    @TypeConverter
    fun fromLastReadTimestamps(map: Map<String, Long>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    @TypeConverter
    fun toLastReadTimestamps(json: String): Map<String, Long> {
        val obj = JSONObject(json)
        return obj.keys().asSequence().associateWith { key -> obj.getLong(key) }
    }
}