package com.example.bustrackv2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrackv2.adapters.RouteScheduleAdapter
import com.example.bustrackv2.databinding.ActivityRouteScheduleBinding
import com.example.bustrackv2.models.RouteSchedule
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RouteScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRouteScheduleBinding
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TAG = "RouteScheduleActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDatePicker()
        setupFromDropdown()
        setupRecyclerView()
        setupSearchButton()
    }

    private fun setupDatePicker() {
        binding.dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    binding.dateInput.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupFromDropdown() {
        val locations = listOf("A", "B", "C", "D", "E")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locations)
        binding.fromDropdown.setAdapter(adapter)
    }

    private fun setupRecyclerView() {
        binding.scheduleRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            val from = binding.fromDropdown.text.toString()
            val date = binding.dateInput.text.toString()

            if (from.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please select both location and date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            binding.loadingProgress.visibility = View.VISIBLE
            binding.scheduleRecyclerView.visibility = View.GONE

            searchSchedules(from, date)
        }
    }

    private fun searchSchedules(from: String, date: String) {
        Log.d(TAG, "Searching schedules for from: $from, date: $date")
        
        db.collection("route_schedule")
            .whereEqualTo("from", from)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Query returned ${documents.size()} documents")
                
                val schedules = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(RouteSchedule::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document: ${doc.id}", e)
                        null
                    }
                }
                
                if (schedules.isEmpty()) {
                    Toast.makeText(this, "No schedules found for $from on $date", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "No schedules found for $from on $date")
                } else {
                    Log.d(TAG, "Found ${schedules.size} schedules")
                }
                
                displaySchedules(schedules)
                
                // Hide loading state
                binding.loadingProgress.visibility = View.GONE
                binding.scheduleRecyclerView.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting documents", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Hide loading state
                binding.loadingProgress.visibility = View.GONE
                binding.scheduleRecyclerView.visibility = View.VISIBLE
            }
    }

    private fun displaySchedules(schedules: List<RouteSchedule>) {
        val adapter = RouteScheduleAdapter(schedules) { schedule ->
            val coordinates = schedule.coordinates
            if (coordinates.isEmpty()) {
                Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show()
                return@RouteScheduleAdapter
            }
            
            // Navigate to tracking activity
            val intent = Intent(this, TrackBusActivity::class.java).apply {
                putExtra("vehicle_number", schedule.vehicle_number)
                putExtra("service_id", schedule.service_id)
                putExtra("latitude", coordinates["lat"])
                putExtra("longitude", coordinates["lng"])
            }
            startActivity(intent)
        }
        binding.scheduleRecyclerView.adapter = adapter
    }
} 