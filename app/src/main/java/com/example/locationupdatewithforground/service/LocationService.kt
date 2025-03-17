package com.example.locationupdatewithforground.service

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.renderscript.RenderScript.Priority
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.locationupdatewithforground.MainActivity
import com.example.locationupdatewithforground.R
import com.example.locationupdatewithforground.receiver.GeofenceBroadcastReceiver
import com.example.locationupdatewithforground.viewModel.LocationViewModel
import com.example.locationupdatewithforground.worker.LocationWorker
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LocationService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationViewModel: LocationViewModel


    private val geofenceList = mutableListOf<Geofence>()

    private val geofenceRadius = 100f // 100 meters radius
    private val geofenceTransitionPendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)  // Use FLAG_IMMUTABLE here

       // PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.d("TAG", "onCreate: called")

        // Initialize LocationClient and LocationRequest
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 60000  // 10 seconds
            fastestInterval = 5000  // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Setup geofencing
        setupGeofences()

        // Create notification channel for Android O and above
        createNotificationChannel()
        locationViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(LocationViewModel::class.java)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location = locationResult.lastLocation
                Log.d("TAG", "Location Update: Lat: ${location.latitude}, Lng: ${location.longitude}")
                // Update notification with the new location
                updateNotification(location.latitude, location.longitude)
                locationViewModel.updateLocation(location)
            }
        }

        // Check permissions and request location updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle missing permissions here
            Log.e("TAG", "Required permissions not granted.")
            return
        }

        // Start foreground service
        setLoggerForeground()


    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start location updates when the service starts
        startLocationUpdates()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupGeofences() {
        // Define a geofence for a location (e.g., your office)
        val geofence = Geofence.Builder()
            .setRequestId("GEO_1") // Unique ID for the geofence
            .setCircularRegion(37.421999, -122.084057, geofenceRadius) // Lat, Lng, radius
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Geofence expiration (can be set)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        geofenceList.add(geofence)

        // Add geofence to geofencing client
        val geofencingRequest = GeofencingRequest.Builder()
            .addGeofences(geofenceList)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofenceTransitionPendingIntent)
            .addOnSuccessListener {
                Log.d("TAG", "Geofence added successfully")
            }
            .addOnFailureListener {
                Log.e("TAG", "Failed to add geofence: ${it.message}")
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setLoggerForeground() {
        createNotificationChannel()
        val notification = createNotification("Loading location...")
        startForeground(111, notification)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel {
        val channel = NotificationChannel("ID", "Location Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
        return channel
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, "ID")
            .setContentText(contentText)
            .setContentTitle("Location Service Channel")
            .setContentIntent(getPendingIntent())
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)
            .build()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle missing permissions here
            Log.e("TAG", "Location permissions not granted")
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d("TAG", "Location updates started")
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        Log.d("TAG", "Location updates stopped")
    }

    // This method updates the notification with the new location
    private fun updateNotification(lat: Double, lng: Double) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val contentText = "Lat: $lat, Lng: $lng"
        val notification = createNotification(contentText)

        // Update the notification
        notificationManager?.notify(111, notification)
        Log.d("TAG", "Notification updated with location: Lat: $lat, Lng: $lng")
    }

    override fun onDestroy() {
        Log.d("TAG", "onDestroy: called")
        // Stop location updates if service is destroyed
        stopLocationUpdates()
        geofencingClient.removeGeofences(geofenceTransitionPendingIntent)
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

