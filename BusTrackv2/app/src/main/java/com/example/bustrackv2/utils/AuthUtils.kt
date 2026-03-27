package com.example.bustrackv2.utils

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class OperatorDetails(
    val email: String,
    val emp_id: String,
    val name: String,
    val uid: String? = null  // This will be set after Firebase Auth registration
)

object AuthUtils {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private const val OPERATORS_COLLECTION = "operators"

    fun registerOperator(email: String, empId: String, name: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
            .continueWithTask { authResult ->
                val operatorDetails = OperatorDetails(
                    email = email,
                    emp_id = empId,
                    name = name,
                    uid = authResult.result.user?.uid
                )
                
                // Store operator details in Firestore using emp_id as document ID
                db.collection(OPERATORS_COLLECTION)
                    .document(empId)
                    .set(operatorDetails)
                    .continueWithTask { authResult }
            }
    }

    fun loginOperator(email: String, password: String): Task<AuthResult> {
        // Directly use the email for authentication since it's the actual login identifier
        return auth.signInWithEmailAndPassword(email, password)
    }

    fun isOperatorLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentOperatorId(): String? {
        return auth.currentUser?.email
    }

    fun logoutOperator() {
        auth.signOut()
    }

    fun validateOperatorCredentials(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            return false
        }
        
        // Email should be a valid email address
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)\$"))) {
            return false
        }
        
        // Password should be at least 6 characters and contain at least one special character
        if (password.length < 6 || !password.contains(Regex("[!@#\$%^&*(),.?\":{}|<>]"))) {
            return false
        }
        
        return true
    }
} 