package com.evu.locator.ui.wellwishing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.evu.locator.data.models.AppUser
import com.evu.locator.data.models.Contact
import com.evu.locator.databinding.ItemContactBinding

data class WellWishingItem(val contact: Contact, val user: AppUser?)

class WellWishingAdapter(
    private val onRename: (Contact) -> Unit,
    private val onRemove: (Contact) -> Unit,
    private val onView: (Contact) -> Unit
) : RecyclerView.Adapter<WellWishingAdapter.ViewHolder>() {

    private val items = mutableListOf<WellWishingItem>()

    fun submit(newItems: List<WellWishingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val user = item.user
        val dir = if (user?.activeDirectionForward == true) "Home → College" else "College → Home"
        val routeLabel = if (user?.activeRoute.isNullOrBlank() || user?.sharingEnabled != true) ""
        else " • ${user!!.activeRoute} ($dir)"

        holder.binding.nameText.text = item.contact.displayName + routeLabel
        holder.binding.viewBtn.setOnClickListener { onView(item.contact) }
        holder.binding.renameBtn.setOnClickListener { onRename(item.contact) }
        holder.binding.removeBtn.setOnClickListener { onRemove(item.contact) }
    }

    override fun getItemCount() = items.size
}