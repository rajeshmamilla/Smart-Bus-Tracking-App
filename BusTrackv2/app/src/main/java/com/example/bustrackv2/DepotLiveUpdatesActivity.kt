package com.example.bustrackv2

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrackv2.databinding.ActivityDepotLiveUpdatesBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import android.widget.Toast

class DepotLiveUpdatesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDepotLiveUpdatesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepotLiveUpdatesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val updates = intent.getSerializableExtra("updates") as? ArrayList<HashMap<String, Any>> ?: arrayListOf()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = DepotLiveUpdatesAdapter(updates)

        // Add a Material Toggle Button at the center bottom of the screen
        val toggleButton = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Set Alert"
            isCheckable = true
            isChecked = false
            icon = ContextCompat.getDrawable(this@DepotLiveUpdatesActivity, R.drawable.ic_alert)
            iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = 24
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM
                bottomMargin = 64 // dp, adjust as needed
            }
            setOnClickListener {
                if (isChecked) {
                    // Find the first update with non-empty depot_name and platform_number
                    val validUpdate = updates.firstOrNull {
                        !(it["depot_name"] as? String).isNullOrBlank() && !(it["platform_number"] as? String).isNullOrBlank()
                    }
                    if (validUpdate != null) {
                        val depotName = validUpdate["depot_name"] as String
                        val platformNumber = validUpdate["platform_number"] as String
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                val alert = hashMapOf(
                                    "token" to token,
                                    "depot_name" to depotName,
                                    "platform_number" to platformNumber,
                                    "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )
                                FirebaseFirestore.getInstance().collection("bus_alerts")
                                    .add(alert)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@DepotLiveUpdatesActivity, "Alert set for Depot $depotName, Platform $platformNumber", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@DepotLiveUpdatesActivity, "Failed to set alert: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this@DepotLiveUpdatesActivity, "Failed to get FCM token", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@DepotLiveUpdatesActivity, "No valid depot/platform found to set alert.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        // Add to root FrameLayout (wrap in FrameLayout if needed)
        val rootView = binding.root
        if (rootView is FrameLayout) {
            rootView.addView(toggleButton)
        } else {
            val frame = FrameLayout(this)
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            frame.layoutParams = params
            // Remove rootView from parent if needed
            val parent = rootView.parent as? ViewGroup
            parent?.removeView(rootView)
            frame.addView(rootView)
            frame.addView(toggleButton)
            setContentView(frame)
        }
    }
} 