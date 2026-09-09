package com.safecircle.app.data

import android.content.Context

class SafeCirclePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("safe_circle", Context.MODE_PRIVATE)

    var name: String
        get() = prefs.getString("name", "Me") ?: "Me"
        set(value) = prefs.edit().putString("name", value).apply()

    var circleCode: String
        get() = prefs.getString("circle_code", "246810") ?: "246810"
        set(value) = prefs.edit().putString("circle_code", value).apply()

    var emergencyNumber: String
        get() = prefs.getString("emergency_number", "999") ?: "999"
        set(value) = prefs.edit().putString("emergency_number", value).apply()

    fun contacts(): List<EmergencyContact> = (1..3).mapNotNull { index ->
        val name = prefs.getString("contact_${index}_name", null)
        val phone = prefs.getString("contact_${index}_phone", null)
        if (!name.isNullOrBlank() && !phone.isNullOrBlank()) EmergencyContact(name, phone) else null
    }

    fun saveContact(index: Int, contact: EmergencyContact?) {
        prefs.edit().apply {
            if (contact == null) {
                remove("contact_${index}_name")
                remove("contact_${index}_phone")
            } else {
                putString("contact_${index}_name", contact.name)
                putString("contact_${index}_phone", contact.phone)
            }
        }.apply()
    }
}

data class EmergencyContact(val name: String, val phone: String)
