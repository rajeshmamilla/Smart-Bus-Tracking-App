package com.example.bustrackv2

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrackv2.databinding.ActivityOperatorMainPageBinding
import com.example.bustrackv2.utils.AuthUtils
import com.example.bustrackv2.utils.LocationUtils
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore

class OperatorMainPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOperatorMainPageBinding
    private var isTrackingEnabled = false
    private var allotedVehicle: String? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchAllotedVehicleAndInit()
    }

    private fun fetchAllotedVehicleAndInit() {
        // For demo, using 'driver1' as the operator's document ID. Replace with actual logic if needed.
        val operatorDocId = "driver1"
        db.collection("operators").document(operatorDocId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    allotedVehicle = document.getString("alloted_vehicle")
                }
                setupDropdowns()
                setupButtons()
                checkGPSPermissions()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch operator info", Toast.LENGTH_LONG).show()
                setupDropdowns()
                setupButtons()
                checkGPSPermissions()
            }
    }

    private fun setupDropdowns() {
        // Setup Reached At dropdown
        val locations = listOf("A", "B", "C", "D", "E")
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locations)
        binding.reachedAtDropdown.setAdapter(locationAdapter)

        // Setup Platform Number dropdown
        val platformNumbers = (1..15).map { it.toString() }
        val platformAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platformNumbers)
        binding.platformNumberDropdown.setAdapter(platformAdapter)
    }

    private fun setupButtons() {
        // Logout button
        binding.logoutButton.setOnClickListener {
            AuthUtils.logoutOperator()
            // Navigate back to login screen
            val intent = Intent(this, OperatorLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // End Trip button
        binding.endTripButton.setOnClickListener {
            if (isTrackingEnabled) {
                Toast.makeText(this, "Please stop GPS tracking before ending trip", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: Implement end trip logic
            Toast.makeText(this, "Ending trip...", Toast.LENGTH_SHORT).show()
            finish()
        }

        // GPS Toggle button
        binding.gpsToggleButton.setOnClickListener {
            val newState = !isTrackingEnabled
            handleGPSToggle(newState)
        }

        // Submit button
        binding.submitButton.setOnClickListener {
            handleSubmit()
        }
    }

    private fun handleSubmit() {
        val reachedAt = binding.reachedAtDropdown.text.toString()
        val platformNo = binding.platformNumberDropdown.text.toString()

        if (reachedAt.isEmpty() || platformNo.isEmpty()) {
            Toast.makeText(this, "Please select both location and platform number", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isTrackingEnabled) {
            Toast.makeText(this, "Please enable GPS tracking first", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicleId = allotedVehicle
        if (vehicleId.isNullOrEmpty()) {
            Toast.makeText(this, "No vehicle assigned to operator", Toast.LENGTH_LONG).show()
            return
        }

        // Update Firestore with current_depot and current_platform
        val updates = hashMapOf<String, Any>(
            "current_depot" to reachedAt,
            "current_platform" to platformNo
        )
        db.collection("services")
            .document(vehicleId)
            .update(updates)
            .addOnSuccessListener {
                // Fetch path and expected_platform_numbers to determine next_depot and expected_platform
                db.collection("services").document(vehicleId).get()
                    .addOnSuccessListener { doc ->
                        val path = doc.get("path") as? List<*>
                        val expectedPlatforms = doc.get("expected_platform_numbers") as? List<*>
                        val from = doc.getString("from") ?: ""
                        val to = doc.getString("to") ?: ""
                        val date = doc.getString("date") ?: ""
                        val speed = doc.get("speed") ?: 0
                        if (path != null && expectedPlatforms != null) {
                            val currentIndex = path.indexOf(reachedAt)
                            // Only update if currentIndex is valid and not the last stop
                            if (currentIndex != -1 && currentIndex < path.size - 1 && currentIndex < expectedPlatforms.size - 1) {
                                val nextDepot = path[currentIndex + 1] as? String ?: ""
                                val nextPlatform = expectedPlatforms.getOrNull(currentIndex + 1) as? String ?: ""
                                val nextUpdates = hashMapOf<String, Any>(
                                    "next_depot" to nextDepot,
                                    "expected_platform" to nextPlatform
                                )
                                db.collection("services").document(vehicleId).update(nextUpdates)
                            } else {
                                // End of path, clear next_depot and expected_platform
                                val nextUpdates = hashMapOf<String, Any>(
                                    "next_depot" to "",
                                    "expected_platform" to ""
                                )
                                db.collection("services").document(vehicleId).update(nextUpdates)
                            }
                        }
                        // --- Update depot_live_updates_a for live depot search ---
                        val depotLiveUpdate = hashMapOf(
                            "arrival_time" to "", // You may want to set this from logic or time picker
                            "departure_time" to "", // You may want to set this from logic or time picker
                            "current_service" to vehicleId,
                            "depot_name" to reachedAt,
                            "platform_number" to platformNo,
                            "is_occupied" to true,
                            "last_update" to com.google.firebase.Timestamp.now(),
                            "from" to from,
                            "to" to to
                        )
                        // Use a composite key or a deterministic doc id, here using depot+platform
                        val depotDocId = "${reachedAt}_${platformNo}"
                        db.collection("depot_live_updates_a").document(depotDocId).set(depotLiveUpdate)
                        // --- End update depot_live_updates_a ---
                        Toast.makeText(
                            this,
                            "Location updated: Depot $reachedAt, Platform $platformNo",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Clear the inputs after successful submission
                        binding.reachedAtDropdown.text.clear()
                        binding.platformNumberDropdown.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to fetch path/platforms: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update location: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkGPSPermissions() {
        if (!LocationUtils.isGPSEnabled(this)) {
            Toast.makeText(this, "Please enable GPS in settings", Toast.LENGTH_LONG).show()
            binding.gpsToggleButton.isEnabled = false
            return
        }

        LocationUtils.checkLocationPermission(this)
    }

    private fun handleGPSToggle(isChecked: Boolean) {
        if (isChecked && !LocationUtils.checkLocationPermission(this)) {
            updateToggleState(false)
            return
        }

        isTrackingEnabled = isChecked
        updateToggleState(isChecked)
        
        val message = if (isChecked) "GPS tracking enabled" else "GPS tracking disabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        if (isChecked) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val vehicleId = allotedVehicle
        if (vehicleId.isNullOrEmpty()) {
            Toast.makeText(this, "No vehicle assigned to operator", Toast.LENGTH_LONG).show()
            return
        }
        val locationRequest = LocationRequest.create().apply {
            interval = 2500 // 2.5 seconds
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val coordinates = hashMapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude
                )
                val updates = hashMapOf<String, Any>(
                    "coordinates" to coordinates,
                    "last_updated" to System.currentTimeMillis()
                )
                db.collection("services")
                    .document(vehicleId)
                    .update(updates)
            }
        }
        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }

    private fun updateToggleState(isChecked: Boolean) {
        if (isChecked) {
            binding.gpsOffText.visibility = View.GONE
            binding.gpsOnText.visibility = View.VISIBLE
        } else {
            binding.gpsOffText.visibility = View.VISIBLE
            binding.gpsOnText.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, enable GPS toggle
            binding.gpsToggleButton.isEnabled = true
        } else {
            // Permission denied, disable GPS toggle
            binding.gpsToggleButton.isEnabled = false
            Toast.makeText(this, "Location permission required for GPS tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
} 