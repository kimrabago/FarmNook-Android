package com.ucb.capstone.farmnook.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.databinding.ActivityRegisterBinding
import com.ucb.capstone.farmnook.ui.users.LocationPickerActivity
import com.ucb.capstone.farmnook.viewmodel.auth.RegistrationViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val registrationViewModel: RegistrationViewModel by viewModels()

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private val LOCATION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerBtn.setOnClickListener {
            registerUser()
        }

        binding.signInHere.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        setupPasswordToggle(binding.etPassword)
        setupPasswordToggle(binding.etConfirmPassword)

        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_business) {
                binding.businessName.visibility = View.VISIBLE
                binding.businessLocation.visibility = View.VISIBLE
            } else {
                binding.businessName.visibility = View.GONE
                binding.businessLocation.visibility = View.GONE
            }
        }

        binding.businessLocation.setOnClickListener{
            val intent = Intent(this, LocationPickerActivity::class.java)
            startActivityForResult(intent, LOCATION_REQUEST_CODE)
        }

        observeViewModel()
    }

    // Handle the result from LocationPickerActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedLocation = data?.getStringExtra("selectedLocation")
            val selectedCoordinates = data?.getStringExtra("selectedCoordinates")

            binding.businessLocation.setText(selectedLocation)
            binding.businessLocation.tag = selectedCoordinates  // Store lat,lng as hidden metadata // Update the location input field


            binding.businessLocation.setTag(R.id.business_location_name_tag, selectedLocation)

        }
    }

    private fun observeViewModel() {
        registrationViewModel.registrationStatus.observe(this) { message ->
            showToast(message)
            if (message.contains("successful", ignoreCase = true)) {
                val userType = when (binding.rgUserType.checkedRadioButtonId) {
                    R.id.rb_farmer -> "Farmer"
                    R.id.rb_business -> "Hauler Business Admin"
                    else -> ""
                }
                navigateToLogin(userType)
            }
        }

        registrationViewModel.emailVerificationStatus.observe(this) { message ->
            showToast(message)
        }
    }

    private fun registerUser() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()

        val userType = when (binding.rgUserType.checkedRadioButtonId) {
            R.id.rb_farmer -> "Farmer"
            R.id.rb_business -> "Hauler Business Admin"
            else -> ""
        }

        val businessName = if (userType == "Hauler Business Admin") binding.businessName.text.toString().trim() else ""
        val businessLocation = if (userType == "Hauler Business Admin") {
            val tagValue = binding.businessLocation.tag
            if (tagValue != null && tagValue.toString().lowercase() != "null") tagValue.toString() else null
        } else null

        val locationName = if (userType == "Hauler Business Admin") {
            binding.businessLocation.getTag(R.id.business_location_name_tag)?.toString()
        } else null

        registrationViewModel.registerUser(firstName, lastName, email, password, confirmPass, userType, businessName, businessLocation, locationName)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText) {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width() - editText.paddingEnd)) {
                    togglePasswordVisibility(editText)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        val isCurrentlyVisible = if (editText.id == R.id.et_password) isPasswordVisible else isConfirmPasswordVisible

        if (isCurrentlyVisible) {
            editText.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.pass_icon, 0, R.drawable.eye_closed, 0)
        } else {
            editText.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.pass_icon, 0, R.drawable.eye_open, 0)
        }

        if (editText.id == R.id.et_password) {
            isPasswordVisible = !isPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        editText.setSelection(editText.text.length)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin(userType: String) {
        if (userType == "Hauler Business Admin") {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.hauler_notice_register_dialog)

            val closeButton = dialog.findViewById<Button>(R.id.closeButton)
            closeButton.setOnClickListener {
                goToLoginScreen()
            }

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        } else {
            goToLoginScreen()
        }
    }

    private fun goToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
