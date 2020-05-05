package com.example.honeyimhome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import com.google.gson.Gson

class HoneyImHomeWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sp: SharedPreferences

    override fun doWork(): Result {
        if (!checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS)) {
            return Result.success()
        }

        sp = applicationContext.getSharedPreferences("honeyImHome", Context.MODE_PRIVATE)
        val location = sp.getString("HomeLocation", null)
        val phone = sp.getString("phoneNumber", null)
        if (location == null || phone.isNullOrEmpty()) {
            return Result.success()
        }

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
        return Result.success()
    }

    private fun checkPermissions(vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            if (location.accuracy >= 50) return
            val currentLocation = LocationInfo(location.latitude, location.longitude, location.accuracy)
            val prevLocation = retrieveLocationFromSP("current")
            fusedLocationClient.removeLocationUpdates(this) // stop tracking
            if (prevLocation == null || location.distanceTo(convertToLocation(prevLocation)) <= 50) return
            sp.edit().putString("current", Gson().toJson(currentLocation)).apply() // save to SP
            val homeLocation = retrieveLocationFromSP("HomeLocation")
            if (location.distanceTo(homeLocation?.let { convertToLocation(it) }) < 50) {
                sendSms()
            }
        }
    }

    private fun retrieveLocationFromSP(key: String): LocationInfo? {
        val json: String? = sp.getString(key, "")
        if (json != "") {
            return Gson().fromJson(json, LocationInfo::class.java)
        }
        return null
    }

    private fun sendSms() {
        val intent = Intent()
        intent.action = "POST_PC.ACTION_SEND_SMS"
        intent.putExtra("PHONE", sp.getString("phoneNumber", ""))
        intent.putExtra("CONTENT", "Honey I'm Home!")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun convertToLocation(toBeConverted : LocationInfo): Location {
        val location = Location("")
        location.latitude = toBeConverted.latitude
        location.longitude = toBeConverted.longitude
        return location
    }

}