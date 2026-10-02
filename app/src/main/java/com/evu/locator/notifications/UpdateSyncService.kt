package com.evu.locator.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.data.models.Contact
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UpdateSyncService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        val myUid = FirestoreRepo.myUid ?: return START_STICKY
        scope.launch {
            FirestoreRepo.listenContacts(myUid, "wellWishing").collectLatest { contacts ->
                val currentUids = contacts.map { it.otherUid }.toSet()

                // stop watching anyone no longer in our well-wishing list
                val toRemove = activeListeners.keys.filter { it !in currentUids }
                toRemove.forEach { uid ->
                    activeListeners[uid]?.remove()
                    activeListeners.remove(uid)
                }

                // start watching anyone new
                contacts.forEach { contact ->
                    if (!activeListeners.containsKey(contact.otherUid)) {
                        watchContactUpdates(contact, myUid)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun watchContactUpdates(contact: Contact, myUid: String) {
        val reg = db.collection("users").document(contact.otherUid).collection("updates")
            .whereIn("targetUid", listOf("", myUid))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, _ ->
                val doc = snap?.documents?.firstOrNull() ?: return@addSnapshotListener
                val ts = doc.getLong("timestamp") ?: return@addSnapshotListener
                val text = doc.getString("update") ?: return@addSnapshotListener
                val desc = doc.getString("description") ?: ""

                val prev = LastSeenStore.get(applicationContext, contact.otherUid)
                if (ts > prev) {
                    LastSeenStore.set(applicationContext, contact.otherUid, ts)
                    if (prev != 0L) {
                        FirestoreRepo.getSharingEnabledOnce(contact.otherUid) { sharing ->
                            if (sharing) {
                                NotificationHelper.show(
                                    applicationContext,
                                    title = contact.displayName.ifBlank { "Traveler" },
                                    text = text,
                                    bigText = if (desc.isNotBlank()) "$text\n$desc" else null,
                                    notifId = contact.otherUid.hashCode()
                                )
                            }
                        }
                    }
                }
            }
        activeListeners[contact.otherUid] = reg
    }

    private fun startForegroundNotification() {
        NotificationHelper.ensureChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, "locator_updates")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Locator")
            .setContentText("Watching for updates from people you're well-wishing")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
    }
}