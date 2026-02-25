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

    lateinit var email: EditText
    lateinit var password: EditText
    lateinit var registerBtn: Button
    lateinit var auth: FirebaseAuth

    lateinit var goToLogin: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        registerBtn = findViewById(R.id.registerBtn)

        auth = FirebaseAuth.getInstance()

        registerBtn.setOnClickListener {
            val e = email.text.toString()
            val p = password.text.toString()

            if (e.isNotEmpty() && p.isNotEmpty()) {
                registerUser(e, p)
            } else {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_LONG).show()
            }
        }
        goToLogin = findViewById(R.id.goToLogin)

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private fun registerUser(e: String, p: String) {
        auth.createUserWithEmailAndPassword(e, p)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    auth.currentUser?.sendEmailVerification()

                    Toast.makeText(this, "Registered. Verify your email.", Toast.LENGTH_LONG).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
