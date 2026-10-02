package com.evu.locator.geofence

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.data.LocalStationStore
import com.evu.locator.data.StationStateStore
import com.evu.locator.data.models.Station
import com.evu.locator.notifications.NotificationHelper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationCheckService : Service() {

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationCheckService::class.java))
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, LocationCheckService::class.java))
        }
    }

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var cachedCloudStations: List<Station> = emptyList()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            checkStations(location.latitude, location.longitude)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        FirestoreRepo.fetchCloudStations { cachedCloudStations = it }
        requestUpdates()
        return START_STICKY
    }

    @RequiresPermission(anyOf = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"])
    private fun requestUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 45_000L)
            .setMinUpdateIntervalMillis(30_000L)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) { /* no permission */ }
    }

    private fun checkStations(lat: Double, lng: Double) {
        val myUid = FirestoreRepo.myUid ?: return
        val local = LocalStationStore.getAll(applicationContext)
        val overriddenIds = local.mapNotNull { it.overridesCloudId }.toSet()
        val stations = local + cachedCloudStations.filter { it.id !in overriddenIds }
        val insideSet = StationStateStore.getInsideSet(applicationContext)

        stations.forEach { station ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(lat, lng, station.lat, station.lng, results)
            val isInside = results[0] <= station.radius
            val wasInside = insideSet.contains(station.id)

            if (isInside && !wasInside) {
                FirestoreRepo.postGeofenceUpdate(myUid, station.name, true)
                StationStateStore.markEntered(applicationContext, station.id)
            } else if (!isInside && wasInside) {
                FirestoreRepo.postGeofenceUpdate(myUid, station.name, false)
                StationStateStore.markExited(applicationContext, station.id)
            }
        }
    }

    private fun startForegroundNotification() {
        NotificationHelper.ensureChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, "locator_updates")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Locator")
            .setContentText("Sharing your journey")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(callback)
    }
}