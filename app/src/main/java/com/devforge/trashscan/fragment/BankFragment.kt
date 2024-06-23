package com.devforge.trashscan.fragment

import GridSpacingItemDecoration
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.devforge.trashscan.R
import com.devforge.trashscan.activity.DetailBankActivity
import com.devforge.trashscan.activity.GiftActivity
import com.devforge.trashscan.activity.ScanActivity
import com.devforge.trashscan.adapter.BankAdapter
import com.devforge.trashscan.databinding.FragmentBankBinding
import com.devforge.trashscan.model.Bank
import com.devforge.trashscan.preferences.PreferenceManager
import com.devforge.trashscan.util.PrefUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class BankFragment : Fragment(), BankAdapter.AdapterListener {

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val GRID_SPAN_COUNT = 2
        private const val GRID_SPACING_DP = 12
    }

    private lateinit var binding: FragmentBankBinding
    private lateinit var adapter: BankAdapter
    private val db by lazy { Firebase.firestore }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat: Double = 0.0
    private var userLon: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBankBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupScanButton()
        setupGiftButton()
        setupRecyclerView()
        getUserLocationAndUpdateBanks()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureStatusBar()
        getLocation()
    }

    private fun setupScanButton() {
        binding.btnBarcode.setOnClickListener {
            startActivity(Intent(activity, ScanActivity::class.java))
        }
    }

    private fun setupGiftButton() {
        binding.gift.setOnClickListener {
            startActivity(Intent(activity, GiftActivity::class.java))
        }
    }

    private fun configureStatusBar() {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.statusBarColor = resources.getColor(R.color.primary500, null)
                window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = resources.getColor(R.color.primary500)
            }
        }
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLat = it.latitude
                    userLon = it.longitude
                    getAddress(userLat, userLon)
                } ?: run {
                    binding.location.text = getString(R.string.location_not_available)
                }
            }
        }
    }

    private fun getAddress(lat: Double, lon: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        binding.location.text = if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val city = address.locality ?: address.subAdminArea ?: "Makassar"
            val country = address.countryName
            "$city, $country"
        } else {
            getString(R.string.location_not_available)
        }
    }

    private fun setupRecyclerView() {
        adapter = BankAdapter(arrayListOf(), this)
        binding.adapterBank.layoutManager = GridLayoutManager(context, GRID_SPAN_COUNT)
        binding.adapterBank.adapter = adapter

        val spacingInPixels = (GRID_SPACING_DP * resources.displayMetrics.density).toInt()
        binding.adapterBank.addItemDecoration(GridSpacingItemDecoration(GRID_SPAN_COUNT, spacingInPixels, true))
    }

    private fun getUserLocationAndUpdateBanks() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                userLat = it.latitude
                userLon = it.longitude
                getBank(userLat, userLon)
            } ?: run {
                getBank()
            }
        }
    }

    private fun getBank(userLat: Double = 0.0, userLon: Double = 0.0) {
        binding.progressBar.visibility = View.VISIBLE
        val banks: ArrayList<Bank> = arrayListOf()
        db.collection("bank")
            .get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                result.forEach { doc ->
                    val geoPoint = doc.data["location"] as? GeoPoint
                    val location = geoPoint?.let {
                        "Lat: ${it.latitude}, Lon: ${it.longitude}"
                    } ?: "Location not available"

                    val exchangeValue = (doc.data["exchange"] as? Long)?.toInt() ?: (doc.data["exchange"] as? Int) ?: 0

                    banks.add(
                        Bank(
                            id = doc.reference.id,
                            name = doc.data["name"].toString(),
                            photoUrl = doc.data["photoUrl"].toString(),
                            latitude = doc.data["latitude"].toString(),
                            longitude = doc.data["longitude"].toString(),
                            location = doc.data["location"].toString(),
                            exchange = exchangeValue
                        )
                    )
                }
                adapter.setData(banks, userLat, userLon)
                binding.trashTotal.text = getString(R.string.bank_count, banks.size)
            }
    }

    override fun onClick(bank: Bank) {
        val intent = Intent(requireContext(), DetailBankActivity::class.java).apply {
            putExtra("id", bank.id)
            putExtra("userLat", userLat)
            putExtra("userLon", userLon)
        }
        startActivity(intent)
    }
}

