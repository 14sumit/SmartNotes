package com.example.ccaa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var goToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        registerBtn = findViewById(R.id.registerBtn)
        goToLogin = findViewById(R.id.goToLogin)

        auth = FirebaseAuth.getInstance()

        registerBtn.setOnClickListener {

            val e = email.text.toString().trim()
            val p = password.text.toString().trim()

            if (e.isEmpty()) {
                email.error = "Enter email"
                return@setOnClickListener
            }

            if (p.length < 6) {
                password.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            registerUser(e, p)
        }

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun registerUser(e: String, p: String) {

        auth.createUserWithEmailAndPassword(e, p)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    auth.currentUser?.sendEmailVerification()

                    Toast.makeText(
                        this,
                        "Registered successfully. Verify your email.",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()

                } else {

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}