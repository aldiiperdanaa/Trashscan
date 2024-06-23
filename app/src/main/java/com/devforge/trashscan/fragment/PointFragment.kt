package com.devforge.trashscan.fragment

import android.content.pm.PackageManager
import android.os.Build
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import com.devforge.trashscan.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PointFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val boundsBuilder = LatLngBounds.Builder()
    private lateinit var containerPopup: View
    private lateinit var pointName: TextView
    private lateinit var pointAddress: TextView
    private lateinit var pointStatus: ImageView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_point, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        containerPopup = view.findViewById(R.id.container_popup)
        pointName = view.findViewById(R.id.pointName)
        pointAddress = view.findViewById(R.id.pointAddress)
        pointStatus = view.findViewById(R.id.statusIcon)


        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }

        val btnFocus = view.findViewById<ImageButton>(R.id.btn_focus)
        btnFocus.setOnClickListener {
            focusToUserLocation()
        }

    }

    private fun focusToUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.myLocation?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        } else {
            // Jika tidak ada izin akses lokasi, maka permintaan izin lagi atau tampilkan pesan kesalahan
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.setOnMarkerClickListener { marker -> onMarkerClick(marker) }
        googleMap.setOnMapClickListener { onMapClick() }
        getMyLocation()
        fetchPoints()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    data class Point(
        var id: String? = null,
        var name: String = "",
        var address: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var status: Double = 0.0
    )

    private fun fetchPoints() {
        val db = Firebase.firestore
        db.collection("point")
            .get()
            .addOnSuccessListener { result ->
                val points = mutableListOf<Point>()
                for (document in result) {
                    val point = document.toObject(Point::class.java)
                    points.add(point)
                }
                addManyMarker(points)
            }
            .addOnFailureListener { exception -> }
    }

    private fun addManyMarker(points: List<Point>) {
        points.forEach { point ->
            val latLng = LatLng(point.latitude, point.longitude)
            val marker = googleMap.addMarker(MarkerOptions().position(latLng).title(point.name))

            marker?.tag = point
            boundsBuilder.include(latLng)
        }

        val bounds: LatLngBounds = boundsBuilder.build()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                300
            )
        )

        googleMap.setOnMarkerClickListener { marker ->
            val point = marker.tag as? Point
            point?.let {
                pointName.text = it.name
                pointAddress.text = it.address

                val statusIconResource = when {
                    it.status >= 0.0 && it.status <= 20.0 -> R.drawable.ic_battery_empty
                    it.status >= 21.0 && it.status <= 40.0 -> R.drawable.ic_battery_low
                    it.status >= 41.0 && it.status <= 70.0 -> R.drawable.ic_battery_medium
                    it.status >= 71.0 && it.status <= 90.0 -> R.drawable.ic_battery_high
                    it.status >= 91.0 && it.status <= 100.0 -> R.drawable.ic_battery_full
                    else -> R.drawable.ic_battery_warning
                }

                pointStatus.setImageResource(statusIconResource)

                val colorFilter = when {
                    it.status >= 0.0 && it.status <= 20.0 -> ContextCompat.getColor(requireContext(), R.color.primary500)
                    it.status >= 21.0 && it.status <= 40.0 -> ContextCompat.getColor(requireContext(), R.color.primary500)
                    it.status >= 41.0 && it.status <= 70.0 -> ContextCompat.getColor(requireContext(), R.color.medium)
                    it.status >= 71.0 && it.status <= 90.0 -> ContextCompat.getColor(requireContext(), R.color.high)
                    it.status >= 91.0 && it.status <= 100.0 -> ContextCompat.getColor(requireContext(), R.color.danger)
                    else -> ContextCompat.getColor(requireContext(), R.color.textSecondary)
                }

                pointStatus.setColorFilter(colorFilter)

                containerPopup.visibility = View.VISIBLE
            }
            true
        }
    }


    private fun onMarkerClick(marker: Marker): Boolean {
        val point = marker.tag as? Point
        point?.let {
            pointName.text = it.name
            pointAddress.text = it.address
            containerPopup.visibility = View.VISIBLE
        }
        return true
    }

    private fun onMapClick() {
        containerPopup.visibility = View.GONE
    }
}

//status >= 0.0 && status <= 20.0 -> R.drawable.ic_battery_empty
//status >= 21.0 && status <= 40.0 -> R.drawable.ic_battery_low
//status >= 41.0 && status<= 70.0 -> R.drawable.ic_battery_medium
//status >= 71.0 && status<= 90.0 -> R.drawable.ic_battery_high
//status >= 91.0 && status<= 10.00 -> R.drawable.ic_battery_full
