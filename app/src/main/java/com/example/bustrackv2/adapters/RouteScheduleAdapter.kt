package com.example.bustrackv2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrackv2.R
import com.example.bustrackv2.models.RouteSchedule

class RouteScheduleAdapter(
    private val schedules: List<RouteSchedule>,
    private val onTrackClick: (RouteSchedule) -> Unit
) : RecyclerView.Adapter<RouteScheduleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val busDetails: TextView = view.findViewById(R.id.busDetailsText)
        val departureTime: TextView = view.findViewById(R.id.departureTimeText)
        val platformNumber: TextView = view.findViewById(R.id.platformNumberText)
        val trackButton: Button = view.findViewById(R.id.trackButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.busDetails.text = "${schedule.bus_type}\n${schedule.vehicle_number}"
        holder.departureTime.text = schedule.departure_time
        holder.platformNumber.text = schedule.platform_number
        
        holder.trackButton.setOnClickListener {
            onTrackClick(schedule)
        }
    }

    override fun getItemCount() = schedules.size
} 