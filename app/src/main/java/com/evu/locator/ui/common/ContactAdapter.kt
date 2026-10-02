package com.evu.locator.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.evu.locator.data.models.Contact
import com.evu.locator.databinding.ItemContactBinding

class ContactAdapter(
    private val showView: Boolean = false,
    private val onRename: (Contact) -> Unit,
    private val onRemove: (Contact) -> Unit,
    private val onView: ((Contact) -> Unit)? = null
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private val items = mutableListOf<Contact>()

    fun submit(newItems: List<Contact>) {
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
        holder.binding.nameText.text = item.displayName
        holder.binding.viewBtn.visibility = if (showView) android.view.View.VISIBLE else android.view.View.GONE
        holder.binding.viewBtn.setOnClickListener { onView?.invoke(item) }
        holder.binding.renameBtn.setOnClickListener { onRename(item) }
        holder.binding.removeBtn.setOnClickListener { onRemove(item) }
    }

    override fun getItemCount() = items.size
}