package com.example.bustrackv2.adapters

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrackv2.databinding.ItemScheduleBinding
import com.example.bustrackv2.models.BusSchedule
import com.google.android.material.button.MaterialButton

class ScheduleAdapter(private val schedules: List<BusSchedule>) : 
    RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private val TAG = "ScheduleAdapter"

    class ScheduleViewHolder(private val binding: ItemScheduleBinding) : 
        RecyclerView.ViewHolder(binding.root) {
            
        fun bind(schedule: BusSchedule, position: Int) {
            try {
                Log.d("ScheduleAdapter", "Binding schedule at position $position")
                Log.d("ScheduleAdapter", "Schedule data: $schedule")
                
                // Set departure time with icon
                binding.departureTimeText.text = "🕒 ${schedule.departure_time}"
                
                // Set bus type with premium styling
                binding.busTypeText.text = schedule.bus_type
                
                // Set vehicle number and platform with icons
                binding.busNumberText.text = "🚌 ${schedule.vehicle_number}"
                binding.platformText.text = "🚏 Platform ${schedule.platform_number}"
                
                // Set service ID in a subtle way
                binding.seatsText.text = "Service: ${schedule.service_id}"

                // Set run_on info
                if (schedule.run_on.isNotEmpty()) {
                    binding.runOnText.text = "Runs on: ${schedule.run_on}"
                    binding.runOnText.visibility = android.view.View.VISIBLE
                } else {
                    binding.runOnText.visibility = android.view.View.GONE
                }

                // Reset button state
                binding.trackButton.apply {
                    // First reset all properties
                    isEnabled = true
                    text = "Track Bus"
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    setTextColor(Color.WHITE)
                    setOnClickListener(null)
                }

                // Now set the state based on is_active
                Log.d("ScheduleAdapter", "Setting button state for bus ${schedule.vehicle_number}, is_active: ${schedule.is_active}")
                
                if (!schedule.is_active) {
                    binding.trackButton.apply {
                        isEnabled = false
                        text = "Not Running"
                        setBackgroundColor(Color.parseColor("#9E9E9E"))
                        setTextColor(Color.parseColor("#616161"))
                    }
                } else {
                    binding.trackButton.setOnClickListener {
                        val context = binding.root.context
                        val intent = android.content.Intent(context, com.example.bustrackv2.TrackBusActivity::class.java)
                        intent.putExtra("vehicleNumber", schedule.vehicle_number)
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScheduleAdapter", "Error binding schedule at position $position: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]
        Log.d(TAG, "Binding position $position: vehicle=${schedule.vehicle_number}, active=${schedule.is_active}")
        holder.bind(schedule, position)
    }

    override fun getItemCount() = schedules.size
} 