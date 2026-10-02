package com.evu.locator.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.data.LocalStationStore
import com.evu.locator.data.StationStateStore
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.FirebaseFirestore

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode))
            return
        }

        val transition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val entered = transition == Geofence.GEOFENCE_TRANSITION_ENTER
        if (!entered && transition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        val myUid = FirestoreRepo.myUid ?: return
        val pendingResult = goAsync()

        for (geofence in triggeringGeofences) {
            val local = LocalStationStore.getById(context, geofence.requestId)
            if (local != null) {
                FirestoreRepo.postGeofenceUpdate(myUid, local.name, entered)
                if (entered) StationStateStore.markEntered(context, geofence.requestId)
                else StationStateStore.markExited(context, geofence.requestId)
            } else {
                // fall back to the cloud "stations" collection by doc id
                FirebaseFirestore.getInstance().collection("stations").document(geofence.requestId).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: geofence.requestId
                        FirestoreRepo.postGeofenceUpdate(myUid, name, entered)
                        if (entered) StationStateStore.markEntered(context, geofence.requestId)
                        else StationStateStore.markExited(context, geofence.requestId)
                        pendingResult.finish()
                    }
                    .addOnFailureListener { pendingResult.finish() }
                continue
            }
        }
        pendingResult.finish()
    }
}