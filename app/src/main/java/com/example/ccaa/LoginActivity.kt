package com.example.ccaa

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var email: EditText
    lateinit var password: EditText
    lateinit var loginBtn: Button
    lateinit var forgetPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email = findViewById(R.id.loginEmail)
        password = findViewById(R.id.loginPassword)
        loginBtn = findViewById(R.id.loginBtn)
        forgetPassword = findViewById(R.id.forgetPassword)

        loginBtn.setOnClickListener {
            val e = email.text.toString()
            val p = password.text.toString()

            if (e.isNotEmpty() && p.isNotEmpty()) {
                loginUser(e, p)
            }
        }

        forgetPassword.setOnClickListener {
            val e = email.text.toString()
            if (e.isNotEmpty()) {
                auth.sendPasswordResetEmail(e)
                Toast.makeText(this, "Reset link sent", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(this, "Verify your email first", Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_LONG).show()
                }
            }
    }
}
