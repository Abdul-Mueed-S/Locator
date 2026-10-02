package com.evu.locator.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.evu.locator.data.models.AppUser
import com.evu.locator.data.models.Contact
import com.evu.locator.databinding.ItemHomeTravelerCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class HomeItem(val contact: Contact, val user: AppUser?)

class TravelerCardAdapter(
    private val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<TravelerCardAdapter.ViewHolder>() {

    private val items = mutableListOf<HomeItem>()

    fun submit(newItems: List<HomeItem>) {
        items.clear()
        items.addAll(newItems.filter { it.user?.sharingEnabled == true })
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemHomeTravelerCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeTravelerCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val user = item.user

        val dir = if (user?.activeDirectionForward == true) "Home → College" else "College → Home"
        val routeLabel = if (user?.activeRoute.isNullOrBlank()) "" else " • ${user!!.activeRoute} ($dir)"
        holder.binding.nameText.text = item.contact.displayName + routeLabel

        holder.binding.statusText.text = user?.lastStatus?.ifBlank { "No updates yet" } ?: "No updates yet"

        val ts = user?.lastTimestamp ?: 0L
        holder.binding.timeAgoText.text = if (ts > 0) formatTimeAgo(ts) else ""

        holder.itemView.setOnClickListener { onClick(item.contact) }
    }

    override fun getItemCount() = items.size

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            mins < 1 -> "just now"
            mins < 60 -> "$mins min ago"
            else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }
}