package com.evu.locator.data.models

data class TravelerUpdate(
    val id: String = "",
    val update: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    val source: String = "manual",
    val targetUid: String = "" // empty = broadcast to all well-wishers
) {
    fun notificationText(): String =
        if (description.isNotBlank()) "${update}___<---Detail" else update
}