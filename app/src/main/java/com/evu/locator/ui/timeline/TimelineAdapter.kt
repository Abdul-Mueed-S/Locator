package com.evu.locator.ui.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.evu.locator.data.models.TravelerUpdate
import com.evu.locator.databinding.ItemTimelineEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimelineAdapter : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    private val items = mutableListOf<TravelerUpdate>()
    private val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun submit(newItems: List<TravelerUpdate>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemTimelineEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimelineEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.updateText.text = item.update
        if (item.description.isNotBlank()) {
            holder.binding.descriptionText.visibility = android.view.View.VISIBLE
            holder.binding.descriptionText.text = item.description
        } else {
            holder.binding.descriptionText.visibility = android.view.View.GONE
        }
        holder.binding.timeText.text = fmt.format(Date(item.timestamp))

        val dotDrawable = if (position == 0)
            com.evu.locator.R.drawable.dot_filled
        else
            com.evu.locator.R.drawable.dot_done
        holder.binding.dotView.setBackgroundResource(dotDrawable)
    }

    override fun getItemCount() = items.size
}