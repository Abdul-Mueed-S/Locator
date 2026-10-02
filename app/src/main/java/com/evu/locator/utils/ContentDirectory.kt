package com.evu.locator.utils

object ContactDirectory {
    private val names = mutableMapOf<String, String>()

    fun put(uid: String, name: String) {
        if (name.isNotBlank()) names[uid] = name
    }

    fun get(uid: String): String = names[uid] ?: "Unknown"
}