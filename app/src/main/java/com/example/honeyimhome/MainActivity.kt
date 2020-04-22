package com.example.honeyimhome

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson


const val PERMISSION_ID = 0

class MainActivity : AppCompatActivity() {

    private lateinit var currentLocationInfo: TextView
    private lateinit var homeLocationInfo: TextView
    private lateinit var homeTitle: TextView
    private lateinit var setHome: Button
    private lateinit var clearHome: Button
    private lateinit var trackingButton: Button

    private lateinit var tracker: LocationTracker
    private lateinit var sp: SharedPreferences
    private lateinit var broadCastReceiver: BroadcastReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tracker = LocationTracker(this, LocationServices.getFusedLocationProviderClient(this))
        sp = getSharedPreferences("homeLocation", Context.MODE_PRIVATE)

        initViews()
        initButtons()

        initBroadcastReceiver()
        retrieveHomeFromSP()
    }

    private fun retrieveHomeFromSP() {
        val json: String? = sp.getString("HomeLocation", "")
        if (json != "") {
            tracker.currentLocation = Gson().fromJson<LocationInfo>(json, LocationInfo::class.java)
            this.setHome.performClick()
        }
    }

    private fun initBroadcastReceiver() {
        this.broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    "new_location" -> {
                        onLocationUpdate()
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction("new_location")
        this.registerReceiver(broadCastReceiver, intentFilter)
    }

    private fun initViews() {
        this.currentLocationInfo = findViewById(R.id.currentLocationInfoTextView)
        this.homeLocationInfo = findViewById(R.id.homeLocationInfoTextView)
        this.homeTitle = findViewById(R.id.homeLocation)
    }

    @SuppressLint("SetTextI18n")
    private fun initButtons() {
        trackingButton = findViewById(R.id.trackingButton)
        setHome = findViewById(R.id.setHome)
        clearHome = findViewById(R.id.clearHome)

        trackingButton.setOnClickListener {
            if (!checkPermission()) {
                requestPermissions()
            } else if (!isLocationEnabled()) {
                Toast.makeText(this, "Please enable location", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                if (!tracker.isTrackingOn) {
                    trackingButton.text = "Stop tracking location"
                    tracker.startTracking()

                } else {
                    trackingButton.text = "Start tracking location"
                    tracker.stopTracking()
                }
            }
        }

        setHome.setOnClickListener {
            // assuming function was called when location accuracy is smaller than 50 meters
            val home = tracker.currentLocation ?: ""
            sp.edit().putString("HomeLocation", Gson().toJson(home)).apply() // save to SP
            this.homeLocationInfo.text = "Latitude: ${tracker.currentLocation?.latitude}\nLongitude: ${tracker.currentLocation?.longitude}"
            setHomeVisibility(TextView.VISIBLE, Button.VISIBLE)
            setHome.visibility = Button.GONE
        }

        clearHome.setOnClickListener {
            sp.edit().remove("HomeLocation").apply()
            setHomeVisibility(TextView.GONE, Button.GONE)
        }

        setHomeVisibility(TextView.GONE, Button.GONE)
    }

    private fun setHomeVisibility(textVisibility: Int, buttonVisibility: Int) {
        this.homeTitle.visibility = textVisibility
        this.homeLocationInfo.visibility = textVisibility
        this.setHome.visibility = buttonVisibility
        this.clearHome.visibility = buttonVisibility

    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        // checks if the user has turned on location from the setting (assuming user grant the app to use location)
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermissions() {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                trackingButton.performClick()
            }
            else {
                Toast.makeText(applicationContext, "App can't operate without location permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReceiver)
        tracker.fusedLocationClient.removeLocationUpdates(tracker.mLocationCallback)
    }

    fun onLocationUpdate() {
        val currentLocationTitle: TextView = findViewById(R.id.currentLocation)
        currentLocationTitle.visibility = TextView.VISIBLE
        this.currentLocationInfo.visibility = TextView.VISIBLE
        val info = "Latitude: ${tracker.currentLocation?.latitude}\nLongitude: ${tracker.currentLocation?.longitude}\n" +
                "Accuracy: ${tracker.currentLocation?.accuracy}"
        this.currentLocationInfo.text = info
        if (tracker.currentLocation?.accuracy!! < 50) {
            this.setHome.visibility = Button.VISIBLE
        }
    }

}
