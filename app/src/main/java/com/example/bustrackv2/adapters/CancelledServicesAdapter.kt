package com.example.bustrackv2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrackv2.R

// Data class for cancelled service display
data class CancelledServiceDisplay(val vehicleNumber: String, val departureTime: String)

class CancelledServicesAdapter(private val items: List<CancelledServiceDisplay>) : RecyclerView.Adapter<CancelledServicesAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vehicleNumber: TextView = view.findViewById(R.id.cancelledVehicleNumber)
        val departureTime: TextView = view.findViewById(R.id.cancelledDepartureTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cancelled_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.vehicleNumber.text = "Vehicle: ${item.vehicleNumber}"
        holder.departureTime.text = "Time: ${item.departureTime}"
    }

    override fun getItemCount() = items.size
} 