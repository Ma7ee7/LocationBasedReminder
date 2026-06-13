package com.ma7ee7.placereminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("place_reminders", Context.MODE_PRIVATE)

    fun getAll(): List<Reminder> {
        val raw = prefs.getString(KEY_REMINDERS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val reminders = mutableListOf<Reminder>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            reminders += Reminder(
                id = item.optString("id"),
                title = item.optString("title"),
                latitude = item.optDouble("latitude"),
                longitude = item.optDouble("longitude"),
                enabled = item.optBoolean("enabled", true),
                triggered = item.optBoolean("triggered", false)
            )
        }

        return reminders
    }

    fun add(title: String, latitude: Double, longitude: Double) {
        val cleanedTitle = title.trim().ifBlank { "Place reminder" }
        val next = getAll() + Reminder(
            id = UUID.randomUUID().toString(),
            title = cleanedTitle,
            latitude = latitude,
            longitude = longitude
        )
        saveAll(next)
    }

    fun delete(id: String) {
        saveAll(getAll().filterNot { it.id == id })
    }

    fun toggleEnabled(id: String) {
        saveAll(getAll().map { reminder ->
            if (reminder.id == id) reminder.copy(enabled = !reminder.enabled) else reminder
        })
    }

    fun resetTriggered(id: String) {
        saveAll(getAll().map { reminder ->
            if (reminder.id == id) reminder.copy(triggered = false, enabled = true) else reminder
        })
    }

    fun markTriggered(id: String) {
        saveAll(getAll().map { reminder ->
            if (reminder.id == id) reminder.copy(triggered = true) else reminder
        })
    }

    private fun saveAll(reminders: List<Reminder>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            array.put(JSONObject().apply {
                put("id", reminder.id)
                put("title", reminder.title)
                put("latitude", reminder.latitude)
                put("longitude", reminder.longitude)
                put("enabled", reminder.enabled)
                put("triggered", reminder.triggered)
            })
        }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    private companion object {
        const val KEY_REMINDERS = "reminders_json"
    }
}
