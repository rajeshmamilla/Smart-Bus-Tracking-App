package com.example.bustrackv2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrackv2.databinding.ActivityMainBinding
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM", "Token: $token")
            }
        }
    }

    private fun setupButtons() {
        binding.passengerButton.setOnClickListener {
            val intent = Intent(this, PassengerSearchActivity::class.java)
            startActivity(intent)
        }

        binding.operatorButton.setOnClickListener {
            val intent = Intent(this, OperatorLoginActivity::class.java)
            startActivity(intent)
        }
    }
}