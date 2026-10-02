package com.evu.locator.ui.locations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.evu.locator.data.models.Station
import com.evu.locator.databinding.ItemStationBinding

data class DisplayStation(val station: Station, val isCloud: Boolean)

class StationAdapter(
    private val onEdit: (Station) -> Unit,
    private val onDelete: (Station) -> Unit,
    private val onOverride: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.ViewHolder>() {

    private val items = mutableListOf<DisplayStation>()

    fun submit(newItems: List<DisplayStation>) {
        items.clear()
        items.addAll(newItems.sortedBy { it.station.name })
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemStationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val station = item.station
        val tag = if (item.isCloud) " (cloud)" else if (station.overridesCloudId != null) " (override)" else ""
        holder.binding.nameText.text = station.name + tag
        holder.binding.detailsText.text = "Lat: ${station.lat}, Lng: ${station.lng}, Radius: ${station.radius.toInt()}m"

        if (item.isCloud) {
            holder.binding.editBtn.text = "Override"
            holder.binding.editBtn.setOnClickListener { onOverride(station) }
            holder.binding.deleteBtn.visibility = View.GONE
        } else {
            holder.binding.editBtn.text = holder.itemView.context.getString(com.evu.locator.R.string.edit)
            holder.binding.editBtn.setOnClickListener { onEdit(station) }
            holder.binding.deleteBtn.visibility = View.VISIBLE
            holder.binding.deleteBtn.setOnClickListener { onDelete(station) }
        }
    }

    override fun getItemCount() = items.size
}