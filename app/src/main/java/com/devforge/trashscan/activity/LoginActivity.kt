package com.devforge.trashscan.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.devforge.trashscan.R
import com.devforge.trashscan.databinding.ActivityLoginBinding
import com.devforge.trashscan.model.User
import com.devforge.trashscan.preferences.PreferenceManager
import com.devforge.trashscan.util.timestampToString
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore

class LoginActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val db by lazy { Firebase.firestore }
    private val pref by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(R.color.backgroundPrimary)
        window.navigationBarColor = resources.getColor(R.color.backgroundSecondary)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            if (isRequired()) login()
            else Toast.makeText(applicationContext, "Isi data dengan lengkap terlebih dahulu", Toast.LENGTH_SHORT).show()
        }

        binding.etUsername.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        updateRegisterButtonState()

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun progress(progress: Boolean) {
        binding.alertLogin.visibility = View.GONE
        when (progress) {
            true -> {
                binding.btnLogin.text = "Memuat..."
                binding.btnLogin.setBackgroundResource(R.drawable.btn_primary_disable)
                binding.btnLogin.setTextColor(resources.getColor(R.color.textTertiary))
                binding.btnLogin.isEnabled = false
            }
            false -> {
                binding.btnLogin.text = "Masuk"
                binding.btnLogin.setBackgroundResource(R.drawable.btn_primary)
                binding.btnLogin.setTextColor(resources.getColor(R.color.white))
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun login() {
        progress(true)
        db.collection("user")
            .whereEqualTo("username", binding.etUsername.text.toString())
            .whereEqualTo("password", binding.etPassword.text.toString())
            .get()
            .addOnSuccessListener { result ->
                progress(false)
                if (result.isEmpty) binding.alertLogin.visibility = View.VISIBLE
                else {
                    result.forEach{ document ->
                        saveSession(
                            User(
                                fullname = document.data["fullname"].toString(),
                                username = document.data["username"].toString(),
                                password = document.data["password"].toString(),
                                created = document.data["created"] as Timestamp
                            )
                        )
                    }
                    Toast.makeText(applicationContext, "Berhasil masuk", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
    }

    private fun isRequired(): Boolean {
        return (
                binding.etUsername.text.toString().isNotEmpty() &&
                        binding.etPassword.text.toString().isNotEmpty()
                )
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateRegisterButtonState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun updateRegisterButtonState() {
        val isEnabled = isRequired()
        binding.btnLogin.isEnabled = isEnabled
        if (isEnabled) {
            binding.btnLogin.setBackgroundResource(R.drawable.btn_primary)
            binding.btnLogin.setTextColor(resources.getColor(R.color.white))
        } else {
            binding.btnLogin.setBackgroundResource(R.drawable.btn_primary_disable)
            binding.btnLogin.setTextColor(resources.getColor(R.color.textTertiary))
        }
    }

    private fun saveSession(user: User) {
        Log.e("LoginActivity", user.toString())
        pref.put("pref_is_login", 1)
        pref.put("pref_fullname", user.fullname)
        pref.put("pref_username", user.username)
        pref.put("pref_password", user.password)
        pref.put("pref_date", timestampToString(user.created)!!)
    }
}
