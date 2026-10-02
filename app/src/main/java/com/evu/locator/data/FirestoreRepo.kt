package com.evu.locator.data

import com.evu.locator.data.models.AppUser
import com.evu.locator.data.models.Contact
import com.evu.locator.data.models.Station
import com.evu.locator.data.models.TravelerUpdate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object FirestoreRepo {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    val myUid: String?
        get() = auth.currentUser?.uid

    fun signInAnonymouslyIfNeeded(onDone: (String?) -> Unit) {
        val current = auth.currentUser
        if (current != null) {
            ensureUserDoc(current.uid)
            onDone(current.uid)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener {
                val uid = it.user?.uid
                if (uid != null) ensureUserDoc(uid)
                onDone(uid)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreRepo", "Anonymous sign-in failed", e)
                onDone(null)
            }
    }

    private fun ensureUserDoc(uid: String) {
        val ref = db.collection("users").document(uid)
        ref.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "uid" to uid,
                        "sharingEnabled" to false,
                        "activeRoute" to "",
                        "activeDirectionForward" to true,
                        "lastStatus" to "",
                        "lastTimestamp" to 0L
                    )
                )
            }
        }
    }

    fun listenUserDoc(uid: String): Flow<AppUser?> = callbackFlow {
        val reg = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    trySend(
                        AppUser(
                            uid = uid,
                            sharingEnabled = snap.getBoolean("sharingEnabled") ?: false,
                            activeRoute = snap.getString("activeRoute") ?: "",
                            activeDirectionForward = snap.getBoolean("activeDirectionForward") ?: true,
                            lastStatus = snap.getString("lastStatus") ?: "",
                            lastTimestamp = snap.getLong("lastTimestamp") ?: 0L
                        )
                    )
                } else {
                    trySend(null)
                }
            }
        awaitClose { reg.remove() }
    }

    fun setSharingEnabled(uid: String, enabled: Boolean) {
        db.collection("users").document(uid).update("sharingEnabled", enabled)
    }

    fun setActiveRoute(uid: String, route: String, forward: Boolean) {
        db.collection("users").document(uid).update(
            mapOf("activeRoute" to route, "activeDirectionForward" to forward)
        )
    }

    fun postUpdate(uid: String, update: String, description: String?, timestamp: Long, targetUid: String = "") {
        val data = mapOf(
            "update" to update,
            "description" to (description ?: ""),
            "timestamp" to timestamp,
            "source" to "manual",
            "targetUid" to targetUid
        )
        db.collection("users").document(uid).collection("updates").add(data)
        if (targetUid.isEmpty()) {
            db.collection("users").document(uid).update(
                mapOf("lastStatus" to update, "lastTimestamp" to timestamp)
            )
        }
    }

    fun postGeofenceUpdate(uid: String, stationName: String, entered: Boolean) {
        val text = if (entered) "Reached $stationName" else "Left $stationName"
        val ts = System.currentTimeMillis()
        val data = mapOf(
            "update" to text,
            "description" to "",
            "timestamp" to ts,
            "source" to "geofence",
            "targetUid" to ""
        )
        db.collection("users").document(uid).collection("updates").add(data)
        db.collection("users").document(uid).update(
            mapOf("lastStatus" to text, "lastTimestamp" to ts)
        )
    }

    fun listenAllUpdates(uid: String): Flow<List<TravelerUpdate>> = callbackFlow {
        val reg = db.collection("users").document(uid).collection("updates")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap.toUpdateList())
            }
        awaitClose { reg.remove() }
    }

    fun listenUpdatesForViewer(travelerUid: String, viewerUid: String): Flow<List<TravelerUpdate>> = callbackFlow {
        val reg = db.collection("users").document(travelerUid).collection("updates")
            .whereIn("targetUid", listOf("", viewerUid))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap.toUpdateList())
            }
        awaitClose { reg.remove() }
    }

    private fun com.google.firebase.firestore.QuerySnapshot?.toUpdateList(): List<TravelerUpdate> {
        return this?.documents?.map { doc ->
            TravelerUpdate(
                id = doc.id,
                update = doc.getString("update") ?: "",
                description = doc.getString("description") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                source = doc.getString("source") ?: "manual",
                targetUid = doc.getString("targetUid") ?: ""
            )
        } ?: emptyList()
    }

    fun listenContacts(myUid: String, role: String): Flow<List<Contact>> = callbackFlow {
        val reg = db.collection("users").document(myUid).collection("contacts")
            .whereEqualTo("role", role)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { doc ->
                    Contact(
                        otherUid = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        role = doc.getString("role") ?: "",
                        connectedAt = doc.getLong("connectedAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun renameContact(myUid: String, otherUid: String, newName: String) {
        db.collection("users").document(myUid).collection("contacts")
            .document(otherUid).update("displayName", newName)
    }

    fun removeContact(myUid: String, otherUid: String) {
        val batch = db.batch()
        batch.delete(db.collection("users").document(myUid).collection("contacts").document(otherUid))
        batch.delete(db.collection("users").document(otherUid).collection("contacts").document(myUid))
        batch.commit()
    }

    fun connect(myUid: String, otherUid: String, nameIGiveThem: String, onDone: (Boolean) -> Unit) {
        if (myUid == otherUid) {
            onDone(false)
            return
        }
        val now = System.currentTimeMillis()
        val myContactRef = db.collection("users").document(myUid).collection("contacts").document(otherUid)
        val theirContactRef = db.collection("users").document(otherUid).collection("contacts").document(myUid)

        val batch = db.batch()
        batch.set(myContactRef, mapOf("displayName" to nameIGiveThem, "role" to "wellWishing", "connectedAt" to now))
        batch.set(theirContactRef, mapOf("displayName" to "Well-wisher", "role" to "wellWisher", "connectedAt" to now))
        batch.commit()
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    // Stations manually added via the Firebase console ("cloud" stations)
    fun fetchCloudStations(onDone: (List<Station>) -> Unit) {
        db.collection("stations").get().addOnSuccessListener { snap ->
            val list = snap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val lat = doc.getDouble("lat") ?: return@mapNotNull null
                val lng = doc.getDouble("lng") ?: return@mapNotNull null
                val radius = doc.getDouble("radius")?.toFloat() ?: 300f
                Station(id = doc.id, name = name, lat = lat, lng = lng, radius = radius)
            }
            onDone(list)
        }.addOnFailureListener { onDone(emptyList()) }
    }

    fun getSharingEnabledOnce(uid: String, onDone: (Boolean) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { onDone(it.getBoolean("sharingEnabled") ?: false) }
            .addOnFailureListener { onDone(false) }
    }
}