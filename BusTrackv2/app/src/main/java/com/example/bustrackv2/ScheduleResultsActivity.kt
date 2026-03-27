package com.example.bustrackv2

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrackv2.adapters.ScheduleAdapter
import com.example.bustrackv2.adapters.CancelledServicesAdapter
import com.example.bustrackv2.adapters.CancelledServiceDisplay
import com.example.bustrackv2.databinding.ActivityScheduleResultsBinding
import com.example.bustrackv2.databinding.DialogCancelledServicesBinding
import com.example.bustrackv2.models.BusSchedule
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleResultsBinding
    private lateinit var db: FirebaseFirestore
    private val TAG = "ScheduleResults"
    private var allSchedules: List<BusSchedule> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get search parameters from intent
        val fromLocation = intent.getStringExtra("fromLocation") ?: ""
        val toLocation = intent.getStringExtra("toLocation") ?: ""
        val date = intent.getStringExtra("date") ?: ""

        // Set route text
        binding.routeText.text = "Route: $fromLocation to $toLocation"

        // Setup RecyclerView
        binding.schedulesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup cancelled services button
        binding.showCancelledButton.setOnClickListener {
            showCancelledServicesDialog()
        }

        // Search for schedules
        searchSchedules(fromLocation, toLocation, date)
    }

    private fun searchSchedules(fromLocation: String, toLocation: String, date: String) {
        showLoading(true)
        
        Log.d(TAG, "Searching schedules for: from=$fromLocation, to=$toLocation, date=$date")
        
        db.collection("schedules")
            .whereEqualTo("from", fromLocation)
            .whereEqualTo("to", toLocation)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                if (documents.isEmpty) {
                    Log.d(TAG, "No schedules found")
                    showNoResults(true)
                    binding.noResultsText.text = "No schedules found for this route and date"
                } else {
                    showNoResults(false)
                    allSchedules = documents.mapNotNull { document ->
                        Log.d(TAG, "Document data: ${document.data}")
                        val schedule = document.toObject(BusSchedule::class.java)
                        Log.d(TAG, "Parsed schedule: vehicle=${schedule.vehicle_number}, active=${schedule.is_active}")
                        schedule
                    }.sortedBy { it.departure_time }
                    
                    Log.d(TAG, "Found ${allSchedules.size} schedules")
                    binding.schedulesRecyclerView.adapter = ScheduleAdapter(allSchedules.filter { it.is_active })
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting schedules", e)
                showLoading(false)
                showNoResults(true)
                binding.noResultsText.text = "Error: ${e.message}"
            }
    }

    private fun showCancelledServicesDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogCancelledServicesBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Fetch cancelled services from Firestore
        db.collection("cancelled_services")
            .get()
            .addOnSuccessListener { documents ->
                val cancelledList = documents.mapNotNull { doc ->
                    val vehicleNumber = doc.getString("vehicle_number") ?: return@mapNotNull null
                    val departureTime = doc.getString("departure_time") ?: "-"
                    CancelledServiceDisplay(vehicleNumber, departureTime)
                }
                if (cancelledList.isEmpty()) {
                    dialogBinding.cancelledServicesRecyclerView.visibility = View.GONE
                    dialogBinding.noCancelledText.visibility = View.VISIBLE
                } else {
                    dialogBinding.cancelledServicesRecyclerView.visibility = View.VISIBLE
                    dialogBinding.noCancelledText.visibility = View.GONE
                    dialogBinding.cancelledServicesRecyclerView.layoutManager = LinearLayoutManager(this)
                    dialogBinding.cancelledServicesRecyclerView.adapter = CancelledServicesAdapter(cancelledList)
                }
            }
            .addOnFailureListener {
                dialogBinding.cancelledServicesRecyclerView.visibility = View.GONE
                dialogBinding.noCancelledText.visibility = View.VISIBLE
            }

        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.schedulesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.noResultsText.visibility = View.GONE
    }

    private fun showNoResults(show: Boolean) {
        binding.noResultsText.visibility = if (show) View.VISIBLE else View.GONE
        binding.schedulesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 