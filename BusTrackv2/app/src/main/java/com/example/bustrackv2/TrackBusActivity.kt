package com.example.bustrackv2

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrackv2.databinding.ActivityTrackBusBinding
import com.example.bustrackv2.models.VehicleLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class TrackBusActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityTrackBusBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var map: GoogleMap
    private var locationListener: ListenerRegistration? = null
    private val TAG = "TrackBusActivity"
    private var vehicleNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get vehicle number from intent
        vehicleNumber = intent.getStringExtra("vehicleNumber") ?: ""
        if (vehicleNumber.isEmpty()) {
            finish()
            return
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Track Bus $vehicleNumber"

        // Setup map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Update UI
        binding.vehicleNumberText.text = "Vehicle: $vehicleNumber"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationListener = db.collection("services")
            .document(vehicleNumber)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val location = snapshot.toObject(VehicleLocation::class.java)
                    location?.let { updateLocation(it) }
                }
            }
    }

    private fun updateLocation(location: VehicleLocation) {
        // Null safety for coordinates
        val coords = location.coordinates
        if (coords.lat == 0.0 && coords.lng == 0.0) {
            Toast.makeText(this, "No location data available for this bus.", Toast.LENGTH_LONG).show()
            return
        }

        // Clear existing markers
        map.clear()

        // Add new marker
        val position = LatLng(coords.lat, coords.lng)
        try {
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("Bus $vehicleNumber")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding marker: ${e.message}")
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("Bus $vehicleNumber")
            )
        }

        // Move camera to new position
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))

        // Update last update time
        val dateFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val timeString = dateFormat.format(Date(location.last_updated))
        binding.lastUpdateText.text = "Last updated: $timeString"

        // Update next stop and expected platform
        binding.nextStopText.text = "Next Stop: ${location.next_depot.ifBlank { "-" }}"
        binding.expectedPlatformText.text = "Expected Platform: ${location.expected_platform.ifBlank { "-" }}"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove()
    }
} 