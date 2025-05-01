package com.ucb.capstone.farmnook.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.databinding.ActivityLoginBinding
import com.ucb.capstone.farmnook.viewmodel.auth.LoginViewModel
import com.ucb.capstone.farmnook.ui.menu.NavigationBar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val loginViewModel: LoginViewModel by viewModels()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val RC_SIGN_IN = 9001

    private var isPasswordVisible = false

    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "LoginPrefs"
    private val KEY_REMEMBER = "remember"
    private val KEY_EMAIL = "email"
    private val KEY_PASSWORD = "password"

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToDashboard(currentUser)
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        setupGoogleSignIn()
        loadRememberedLogin()

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Empty Fields Are Not Allowed!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginViewModel.loginWithEmail(email, password)
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
                saveLoginState() // Save login state when successful
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
                val intent = Intent(this, NavigationBar::class.java).apply {
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

    // Load saved email & password if Remember Me is checked
    private fun loadRememberedLogin() {
        val isChecked = sharedPreferences.getBoolean(KEY_REMEMBER, false)
        binding.checkboxRememberMe.isChecked = isChecked

        if (isChecked) {
            binding.email.setText(sharedPreferences.getString(KEY_EMAIL, ""))
            binding.password.setText(sharedPreferences.getString(KEY_PASSWORD, ""))
        }
    }

    private fun saveLoginState() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        val isChecked = binding.checkboxRememberMe.isChecked

        with(sharedPreferences.edit()) {
            putBoolean(KEY_REMEMBER, isChecked)
            if (isChecked) {
                putString(KEY_EMAIL, email)
                putString(KEY_PASSWORD, password)
            } else {
                remove(KEY_EMAIL)
                remove(KEY_PASSWORD)
            }
            apply()
        }
    }
}
