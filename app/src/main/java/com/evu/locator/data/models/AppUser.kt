package com.evu.locator.data.models

data class AppUser(
    val uid: String = "",
    val sharingEnabled: Boolean = false,
    val activeRoute: String = "",
    val activeDirectionForward: Boolean = true,
    val lastStatus: String = "",
    val lastTimestamp: Long = 0L
)