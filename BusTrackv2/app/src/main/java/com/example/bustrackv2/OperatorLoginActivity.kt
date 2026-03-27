package com.example.bustrackv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrackv2.databinding.ActivityOperatorLoginBinding
import com.example.bustrackv2.utils.AuthUtils
import com.google.firebase.auth.FirebaseAuth

class OperatorLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOperatorLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        // Check if operator is already logged in
        if (AuthUtils.isOperatorLoggedIn()) {
            navigateToMainPage()
            return
        }

        setupLoginButton()
    }

    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            val email = binding.employeeIdEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (!AuthUtils.validateOperatorCredentials(email, password)) {
                val message = when {
                    email.isEmpty() || password.isEmpty() -> "Email and password cannot be empty"
                    !email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)\$")) -> "Please enter a valid email address"
                    password.length < 6 -> "Password must be at least 6 characters long"
                    !password.contains(Regex("[!@#\$%^&*(),.?\":{}|<>]")) -> "Password must contain at least one special character"
                    else -> "Invalid credentials"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            showProgress(true)
            
            AuthUtils.loginOperator(email, password)
                .addOnSuccessListener {
                    showProgress(false)
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToMainPage()
                }
                .addOnFailureListener { e ->
                    showProgress(false)
                    val errorMessage = when {
                        e.message?.contains("password") == true -> "Invalid password"
                        e.message?.contains("no user record") == true -> "No account found with this email"
                        else -> "Login failed: ${e.localizedMessage}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun navigateToMainPage() {
        val intent = Intent(this, OperatorMainPageActivity::class.java)
        startActivity(intent)
        finish() // Close login activity so user can't go back
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
        binding.employeeIdEditText.isEnabled = !show
        binding.passwordEditText.isEnabled = !show
    }
} 