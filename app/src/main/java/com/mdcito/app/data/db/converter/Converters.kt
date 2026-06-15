package com.mdcito.app.data.db.converter

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val array = JSONArray(value)
        return (0 until array.length()).map { array.getString(it) }
    }
}
