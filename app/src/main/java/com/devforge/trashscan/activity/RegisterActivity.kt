package com.devforge.trashscan.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devforge.trashscan.R
import com.devforge.trashscan.databinding.ActivityRegisterBinding
import com.devforge.trashscan.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }
    private val db by lazy { FirebaseFirestore.getInstance() }

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

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnRegister.setOnClickListener {
            if (isRequired()) checkUsername()
            else Toast.makeText(applicationContext, "Isi data dengan lengkap terlebih dahulu", Toast.LENGTH_SHORT).show()
        }

        binding.etFullname.addTextChangedListener(textWatcher)
        binding.etUsername.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        updateRegisterButtonState()

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun progress(progress: Boolean) {
        binding.alertRegister.visibility = View.GONE
        when (progress) {
            true -> {
                binding.btnRegister.text = "Memuat..."
                binding.btnRegister.setBackgroundResource(R.drawable.btn_primary_disable)
                binding.btnRegister.setTextColor(resources.getColor(R.color.textTertiary))
                binding.btnRegister.isEnabled = false
            }
            false -> {
                binding.btnRegister.text = "Daftar"
                binding.btnRegister.setBackgroundResource(R.drawable.btn_primary)
                binding.btnRegister.setTextColor(resources.getColor(R.color.white))
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun isRequired(): Boolean {
        return (
                binding.etFullname.text.toString().isNotEmpty() &&
                        binding.etUsername.text.toString().isNotEmpty() &&
                        binding.etPassword.text.toString().isNotEmpty()
                )
    }

    private fun checkUsername() {
        progress(true)
        db.collection("user")
            .whereEqualTo("username", binding.etUsername.text.toString())
            .get()
            .addOnSuccessListener { result ->
                progress(false)
                if (result.isEmpty) addUser()
                else binding.alertRegister.visibility = View.VISIBLE
            }
    }

    private fun addUser() {
        progress(true)
        val user = User(
            fullname = binding.etFullname.text.toString(),
            username = binding.etUsername.text.toString(),
            password = binding.etPassword.text.toString(),
            created = Timestamp.now()
        )
        db.collection("user")
            .add(user)
            .addOnSuccessListener {
                progress(false)
                Toast.makeText(applicationContext, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
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
        binding.btnRegister.isEnabled = isEnabled
        if (isEnabled) {
            binding.btnRegister.setBackgroundResource(R.drawable.btn_primary)
            binding.btnRegister.setTextColor(resources.getColor(R.color.white))
        } else {
            binding.btnRegister.setBackgroundResource(R.drawable.btn_primary_disable)
            binding.btnRegister.setTextColor(resources.getColor(R.color.textTertiary))
        }
    }
}
