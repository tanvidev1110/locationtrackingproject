package com.example.locationupdatewithforground.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.*
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

class LocationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    override fun doWork(): Result {
        // Check if permissions are granted
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000  // 10 seconds
            fastestInterval = 5000  // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location = locationResult.lastLocation
                Log.d("TAG", "Location Update: Lat: ${location.latitude}, Lng: ${location.longitude}")
                // You can send location to UI or update the database here
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)

        // Return success or failure based on the operation outcome
        return Result.success()
    }
}