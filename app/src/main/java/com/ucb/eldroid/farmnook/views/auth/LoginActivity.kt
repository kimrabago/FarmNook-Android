package com.ucb.eldroid.farmnook.views.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseUser
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.databinding.ActivityLoginBinding
import com.ucb.eldroid.farmnook.views.menu.BottomNavigationBar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ucb.eldroid.farmnook.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val loginViewModel: LoginViewModel by viewModels()
    private val RC_SIGN_IN = 9001

    private var isPasswordVisible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                } else {
                    loginViewModel.loginWithEmail(email, password)
                }
            } else {
                Toast.makeText(this, "Empty Fields Are Not Allowed!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.password.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.password.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= (binding.password.right - drawableEnd.bounds.width() - binding.password.paddingEnd)) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        loginViewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                navigateToDashboard(user)
            }.onFailure {
                Toast.makeText(
                    this,
                    "Incorrect email or password. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        loginViewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess { user ->
                navigateToDashboard(user)
            }.onFailure {
                Toast.makeText(this, "Google sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun navigateToDashboard(user: FirebaseUser?) {
        loginViewModel.getUserData(user) { firstName, lastName, email, userType ->
            if (firstName != null && lastName != null && email != null) {
                val intent = Intent(this, BottomNavigationBar::class.java).apply {
                    putExtra("USER_NAME", "$firstName $lastName")
                    putExtra("USER_EMAIL", email)
                    putExtra("USER_TYPE", userType) // Pass userType
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupGoogleSignIn() {
        googleSignInClient = GoogleSignIn.getClient(
            this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                loginViewModel.authenticateWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.password.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            binding.password.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.pass_icon, 0, R.drawable.eye_closed, 0)
        } else {
            binding.password.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            binding.password.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.pass_icon, 0, R.drawable.eye_open, 0)
        }
        isPasswordVisible = !isPasswordVisible
        binding.password.setSelection(binding.password.text.length)
    }
}
