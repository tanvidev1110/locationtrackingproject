package com.example.locationupdatewithforground

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.locationupdatewithforground.service.LocationService
import com.example.locationupdatewithforground.viewModel.LocationViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var endBtn: Button
    private lateinit var startBtn: Button
    private lateinit var locationTextView: TextView
    private val locationViewModel: LocationViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBtn = findViewById(R.id.start_button)
        endBtn = findViewById(R.id.end_button)
        locationTextView = findViewById(R.id.locationtxt)

        // Check and request permissions
        checkPermissions()

        // Observe LiveData for location updates
        locationViewModel.locationLiveData.observe(this, Observer { location ->
            // Update the UI with the latest location
            location?.let {
                val latLng = "Lat: ${it.latitude}, Lng: ${it.longitude}"
                Log.i("TAG", "MainActivityonCreate: Lattt: ${it.latitude}, Lng: ${it.longitude}")
                locationTextView.text = latLng
            }
        })
    }

    private fun checkPermissions() {
        // Check if the ACCESS_FINE_LOCATION permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

            // Request ACCESS_FINE_LOCATION permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // For Android 10 (API 29) and higher, check for ACCESS_BACKGROUND_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request ACCESS_BACKGROUND_LOCATION permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            )
        }        // For Android 13 (API 33) and higher, check for POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Request POST_NOTIFICATIONS permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions are granted, enable buttons
            enableLocationButtons()
        }
    }

    private fun updateLocationUI(lat: Double, lng: Double) {
        // Make sure to update the UI on the main thread
        runOnUiThread {
            locationTextView.text = "Lat: $lat, Lng: $lng"
        }
    }


    private fun enableLocationButtons() {
        startBtn.setOnClickListener {
            // Start LocationService to begin location updates in the foreground
            val intent = Intent(this, LocationService::class.java)
            startService(intent)
        }

        endBtn.setOnClickListener {
            // Stop LocationService to stop location updates
            val intent = Intent(this, LocationService::class.java)
            stopService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {
            when (requestCode) {
                LOCATION_PERMISSION_REQUEST_CODE -> {
                    // Check if the ACCESS_FINE_LOCATION permission is granted
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Check for background location if required (for Android 10 and above)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            checkPermissions()  // Re-check if background location is granted
                        } else {
                            // Permission granted, enable buttons
                            enableLocationButtons()
                        }
                    } else {
                        // Permission denied, show a message
                        // Show a message explaining that the location permissions are required
                    }
                }
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        enableLocationButtons()
                    } else {
                        // Permission denied for background location
                        // Show a message explaining that the background location is required
                    }
                }
                NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted for notifications
                    enableLocationButtons()
                } else {
                    // Permission denied for notifications
                    // Show a message explaining that notification permissions are required
                }
            }
            }
        }
    }


    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 2
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 3

    }
}