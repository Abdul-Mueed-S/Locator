package com.evu.locator.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.data.LocalStationStore
import com.evu.locator.data.models.Station
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // Merges on-device stations with Firebase-added ones (local overrides win on same cloud id)
    fun registerAllStations(onDone: (Int) -> Unit = {}) {
        val local = LocalStationStore.getAll(context)
        val overriddenCloudIds = local.mapNotNull { it.overridesCloudId }.toSet()
        FirestoreRepo.fetchCloudStations { cloud ->
            val visibleCloud = cloud.filter { it.id !in overriddenCloudIds }
            val combined = local + visibleCloud
            registerGeofences(combined)
            onDone(combined.size)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerGeofences(stations: List<Station>) {
        geofencingClient.removeGeofences(geofencePendingIntent)
        if (stations.isEmpty()) return

        val geofenceList = stations.map { station ->
            Geofence.Builder()
                .setRequestId(station.id)
                .setCircularRegion(station.lat, station.lng, station.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    fun stopGeofencing() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}