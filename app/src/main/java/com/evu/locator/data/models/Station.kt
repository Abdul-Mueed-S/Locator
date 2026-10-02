package com.evu.locator.data.models

data class Station(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radius: Float = 360f,
    val route: String? = null,
    val overridesCloudId: String? = null // set when this local entry overrides a Firebase-added station
)