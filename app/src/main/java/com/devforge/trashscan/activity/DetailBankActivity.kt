package com.devforge.trashscan.activity

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.devforge.trashscan.R
import com.devforge.trashscan.databinding.ActivityDetailBankBinding
import com.devforge.trashscan.model.Bank
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class DetailBankActivity : AppCompatActivity() {
    private final val TAG: String = "DetailBankActivity"
    private val binding by lazy { ActivityDetailBankBinding.inflate(layoutInflater) }
    private val bankId by lazy { intent.getStringExtra("id") }
    private val userLat by lazy { intent.getDoubleExtra("userLat", 0.0) }
    private val userLon by lazy { intent.getDoubleExtra("userLon", 0.0) }
    private val db by lazy { Firebase.firestore }
    private lateinit var bank: Bank

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            insets
        }

        binding.btnRoute.setOnClickListener {
            openGoogleMaps(bank.latitude.toDouble(), bank.longitude.toDouble())
        }
    }

    override fun onStart() {
        super.onStart()
        detailBank()
    }

    private fun detailBank() {
        db.collection("bank")
            .document(bankId!!)
            .get()
            .addOnSuccessListener { result ->
                bank = Bank(
                    id = result.id,
                    name = result["name"].toString(),
                    photoUrl = result["photoUrl"].toString(),
                    latitude = result["latitude"].toString(),
                    longitude = result["longitude"].toString(),
                    location = result["location"].toString(),
                    exchange = (result["exchange"] as Long).toInt()
                )
                binding.bankName.text = bank.name
                binding.bankLocation.text = bank.location

                Glide.with(this@DetailBankActivity)
                    .load(bank.photoUrl)
                    .placeholder(R.drawable.img_placeholder_square)
                    .into(binding.bankPhotoUrl)

                val results = FloatArray(1)
                Location.distanceBetween(userLat, userLon, bank.latitude.toDouble(), bank.longitude.toDouble(), results)
                val distance = results[0] / 1000
                val exchange = bank.exchange.toString()
                binding.bankDistance.text = String.format("%.2f KM", distance)
                binding.bankExchange.text = exchange
            }
    }

    private fun openGoogleMaps(latitude: Double, longitude: Double) {
        val uri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(browserIntent)
    }
}

