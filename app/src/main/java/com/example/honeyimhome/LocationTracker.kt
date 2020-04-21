package com.example.honeyimhome

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult


data class LocationInfo(val latitude: Double, val longitude: Double, val accuracy: Float? = null)

class LocationTracker(private val context: Context, var fusedLocationClient: FusedLocationProviderClient) {

    var isTrackingOn: Boolean = false
    var currentLocation: LocationInfo? = null

    private fun sendBroadcast(message: String) {
        val intent = Intent()
        intent.action = message
        context.sendBroadcast(intent)
    }

    fun startTracking() {
        requestNewLocationData()
//        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
//            val location: Location? = task.result
//            if (location == null) {
//                requestNewLocationData()
//            } else {
//                currentLocation = LocationInfo(location.latitude, location.longitude, location.accuracy)
//                sendBroadcast("new_location")
//            }
//            sendBroadcast("started")
//            isTrackingOn = true
//        }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(mLocationCallback)
        isTrackingOn = false
        sendBroadcast("stopped")
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        // record the location information in runtime (avoid location == null in the following cases:
        // 1. turn off the location and again turn on 2. user never turned on location before using the app (previousLocation == null)
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.numUpdates = 1

        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            currentLocation = LocationInfo(location.latitude, location.longitude, location.accuracy)
            sendBroadcast("new_location")
        }
    }

}