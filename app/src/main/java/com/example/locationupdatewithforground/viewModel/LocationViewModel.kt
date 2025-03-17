package com.example.locationupdatewithforground.viewModel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _locationLiveData = MutableLiveData<Location>()
   // val locationLiveData: LiveData<Location> = _locationLiveData

    val locationLiveData : LiveData<Location>
        get() = _locationLiveData

    // Function to update location
    fun updateLocation(location: Location) {
        Log.i("TAG", "onCreateeeee: Lat: ${location.latitude}, Lng: ${location.longitude}")
        _locationLiveData.value = location
    }
}
