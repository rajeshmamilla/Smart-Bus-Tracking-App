package com.example.bustrackv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrackv2.R

class DepotLiveUpdatesAdapter(private val updates: List<HashMap<String, Any>>) : RecyclerView.Adapter<DepotLiveUpdatesAdapter.ViewHolder>() {
    // Group updates by platform, and for each platform, separate current and up next
    private val groupedUpdates: List<Pair<HashMap<String, Any>?, HashMap<String, Any>?>>

    init {
        // Group by platform_number
        val platformMap = mutableMapOf<String, Pair<HashMap<String, Any>?, HashMap<String, Any>?>>()
        for (update in updates) {
            val platform = update["platform_number"] as? String ?: continue
            val isOccupied = update["is_occupied"] as? Boolean ?: false
            val pair = platformMap[platform] ?: Pair(null, null)
            if (isOccupied) {
                platformMap[platform] = Pair(update, pair.second)
            } else {
                platformMap[platform] = Pair(pair.first, update)
            }
        }
        groupedUpdates = platformMap.values.toList()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val platformNumber: TextView = view.findViewById(R.id.platformNumberText)
        val currentSection: View = view.findViewById(R.id.currentSection)
        val upNextSection: View = view.findViewById(R.id.upNextSection)
        // Current Vehicle
        val currentVehicleNumber: TextView = view.findViewById(R.id.currentVehicleNumberText)
        val currentArrivalTime: TextView = view.findViewById(R.id.currentArrivalTimeText)
        val currentDepartureTime: TextView = view.findViewById(R.id.currentDepartureTimeText)
        // Up Next
        val upNextVehicleNumber: TextView = view.findViewById(R.id.upNextVehicleNumberText)
        val upNextArrivalTime: TextView = view.findViewById(R.id.upNextArrivalTimeText)
        val upNextDepartureTime: TextView = view.findViewById(R.id.upNextDepartureTimeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_depot_live_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (current, upNext) = groupedUpdates[position]
        // Platform number (from either current or upNext)
        val platform = current?.get("platform_number") as? String ?: upNext?.get("platform_number") as? String ?: "-"
        holder.platformNumber.text = "Platform: $platform"
        // Current Vehicle Section
        if (current != null) {
            holder.currentSection.visibility = View.VISIBLE
            holder.currentVehicleNumber.text = "Vehicle: ${current["current_service"] ?: "-"}"
            holder.currentArrivalTime.text = "Arrival: ${current["arrival_time"] ?: "-"}"
            holder.currentDepartureTime.text = "Departure: ${current["departure_time"] ?: "-"}"
        } else {
            holder.currentSection.visibility = View.GONE
        }
        // Up Next Section
        if (upNext != null) {
            holder.upNextSection.visibility = View.VISIBLE
            holder.upNextVehicleNumber.text = "Vehicle: ${upNext["current_service"] ?: "-"}"
            holder.upNextArrivalTime.text = "Arrival: ${upNext["arrival_time"] ?: "-"}"
            holder.upNextDepartureTime.text = "Departure: ${upNext["departure_time"] ?: "-"}"
        } else {
            holder.upNextSection.visibility = View.GONE
        }
    }

    override fun getItemCount() = groupedUpdates.size
} 