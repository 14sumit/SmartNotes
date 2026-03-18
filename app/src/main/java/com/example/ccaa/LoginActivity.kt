package com.example.ccaa

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var forgetPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email = findViewById(R.id.loginEmail)
        password = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginBtn)
        forgetPassword = findViewById(R.id.forgetPassword)

        loginBtn.setOnClickListener {

            val e = email.text.toString().trim()
            val p = password.text.toString().trim()

            if (e.isEmpty()) {
                email.error = "Enter email"
                return@setOnClickListener
            }

            if (p.isEmpty()) {
                password.error = "Enter password"
                return@setOnClickListener
            }

            loginUser(e, p)
        }

        forgetPassword.setOnClickListener {

            val e = email.text.toString().trim()

            if (e.isEmpty()) {
                Toast.makeText(this, "Enter email first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(e)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to email", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loginUser(e: String, p: String) {

        auth.signInWithEmailAndPassword(e, p)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {

                        Toast.makeText(this, "Login Successful", Toast.LENGTH_LONG).show()

                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()

                    } else {

                        Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show()
                    }

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}