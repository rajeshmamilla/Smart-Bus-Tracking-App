package com.example.bustrackv2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrackv2.databinding.ActivityPassengerSearchBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class PassengerSearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPassengerSearchBinding
    private lateinit var db: FirebaseFirestore
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityPassengerSearchBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize Firestore
            db = FirebaseFirestore.getInstance()

            // Setup toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // Setup date picker
            setupDatePicker()

            // Setup platform dropdown
            setupPlatformDropdown()

            // Setup location autocomplete
            setupLocationAutocomplete()

            // Setup click listeners
            setupClickListeners()

            // Chatbot FAB click
            binding.fabChatbot.setOnClickListener {
                val chatbotSheet = PassengerChatbotBottomSheet()
                chatbotSheet.show(supportFragmentManager, "PassengerChatbotBottomSheet")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing activity: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupDatePicker() {
        try {
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                binding.dateInput.setText(dateFormat.format(calendar.time))
            }

            binding.dateInput.setOnClickListener {
                DatePickerDialog(
                    this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up date picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPlatformDropdown() {
        try {
            val platforms = arrayOf("All", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platforms)
            binding.platformNumberDropdown.setAdapter(adapter)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up platform dropdown: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLocationAutocomplete() {
        try {
            // Set up locations based on our Firestore data
            val locations = arrayOf("A", "E")
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locations)
            binding.fromLocationInput.setAdapter(adapter)
            binding.toLocationInput.setAdapter(adapter)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up location autocomplete: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        try {
            binding.depotSearchButton.setOnClickListener {
                if (validateDepotSearch()) {
                    searchDepot()
                }
            }

            binding.busTrackButton.setOnClickListener {
                if (validateBusTracking()) {
                    trackBus()
                }
            }

            binding.scheduleSearchButton.setOnClickListener {
                if (validateScheduleSearch()) {
                    searchSchedule()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up click listeners: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateDepotSearch(): Boolean {
        try {
            val depotName = binding.depotNameInput.text.toString()
            val platformNumber = binding.platformNumberDropdown.text.toString()

            if (depotName.isEmpty()) {
                binding.depotNameLayout.error = "Please enter depot name"
                return false
            }
            if (platformNumber.isEmpty()) {
                binding.platformNumberLayout.error = "Please select platform number"
                return false
            }

            binding.depotNameLayout.error = null
            binding.platformNumberLayout.error = null
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error validating depot search: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun validateBusTracking(): Boolean {
        try {
            val vehicleNumber = binding.vehicleNumberInput.text.toString()

            if (vehicleNumber.isEmpty()) {
                binding.vehicleNumberLayout.error = "Please enter vehicle number"
                return false
            }

            binding.vehicleNumberLayout.error = null
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error validating bus tracking: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun validateScheduleSearch(): Boolean {
        try {
            val fromLocation = binding.fromLocationInput.text.toString()
            val toLocation = binding.toLocationInput.text.toString()
            val date = binding.dateInput.text.toString()

            if (fromLocation.isEmpty()) {
                binding.fromLocationLayout.error = "Please enter from location"
                return false
            }
            if (toLocation.isEmpty()) {
                binding.toLocationLayout.error = "Please enter to location"
                return false
            }
            if (date.isEmpty()) {
                binding.dateLayout.error = "Please select date"
                return false
            }

            binding.fromLocationLayout.error = null
            binding.toLocationLayout.error = null
            binding.dateLayout.error = null
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error validating schedule search: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun searchDepot() {
        try {
            showLoading(true)
            val depotName = binding.depotNameInput.text.toString()
            val platformNumber = binding.platformNumberDropdown.text.toString()

            val query = db.collection("depot_live_updates_a")
                .whereEqualTo("depot_name", depotName)
            val finalQuery = if (platformNumber == "All") query else query.whereEqualTo("platform_number", platformNumber)
            finalQuery.get()
                .addOnSuccessListener { documents ->
                    showLoading(false)
                    if (documents.isEmpty) {
                        Toast.makeText(this, "No live updates found for this depot/platform", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this, DepotLiveUpdatesActivity::class.java)
                        val updates = ArrayList<HashMap<String, Any>>()
                        for (doc in documents) {
                            updates.add(doc.data as HashMap<String, Any>)
                        }
                        intent.putExtra("updates", updates)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error fetching live updates: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            showLoading(false)
            Toast.makeText(this, "Error searching depot: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun trackBus() {
        try {
            showLoading(true)
            val vehicleNumber = binding.vehicleNumberInput.text.toString()

            db.collection("services")
                .document(vehicleNumber)
                .get()
                .addOnSuccessListener { document ->
                    showLoading(false)
                    if (!document.exists()) {
                        Toast.makeText(this, "Bus not found", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this, TrackBusActivity::class.java)
                        intent.putExtra("vehicleNumber", vehicleNumber)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error tracking bus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            showLoading(false)
            Toast.makeText(this, "Error tracking bus: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchSchedule() {
        try {
            val fromLocation = binding.fromLocationInput.text.toString()
            val toLocation = binding.toLocationInput.text.toString()
            val date = binding.dateInput.text.toString()

            val intent = Intent(this, ScheduleResultsActivity::class.java).apply {
                putExtra("fromLocation", fromLocation)
                putExtra("toLocation", toLocation)
                putExtra("date", date)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error launching schedule search: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 