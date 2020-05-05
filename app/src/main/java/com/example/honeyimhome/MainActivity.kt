package com.example.honeyimhome

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

const val PERMISSION_ID_LOCATION = 0
const val PERMISSION_ID_SMS = 1

class MainActivity : AppCompatActivity() {

    private lateinit var currentLocationInfo: TextView
    private lateinit var homeLocationInfo: TextView
    private lateinit var homeTitle: TextView

    private lateinit var tracker: LocationTracker
    private lateinit var sp: SharedPreferences
    private lateinit var broadCastReceiver: BroadcastReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp = getSharedPreferences("honeyImHome", Context.MODE_PRIVATE)
        tracker = LocationTracker(this, LocationServices.getFusedLocationProviderClient(this), sp)


        initViews()
        initButtons()

        initBroadcastReceiver()
        retrieveHomeFromSP()
    }

    private fun initViews() {
        this.currentLocationInfo = findViewById(R.id.currentLocationInfoTextView)
        this.homeLocationInfo = findViewById(R.id.homeLocationInfoTextView)
        this.homeTitle = findViewById(R.id.homeLocation)
    }

    @SuppressLint("SetTextI18n")
    private fun initButtons() {
        trackingButton.setOnClickListener { onTrackingButton() }
        setHome.setOnClickListener { onSetHome() }
        clearHome.setOnClickListener { onClearHome() }
        setPhoneButton.setOnClickListener { onSetPhoneButton() }
        testSms.setOnClickListener { onTestSms() }

        testSms.visibility = View.INVISIBLE
        setHomeVisibility(TextView.GONE, Button.GONE)
    }

    private fun initBroadcastReceiver() {
        this.broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    "new_location" -> { onLocationUpdate() }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction("new_location")
        this.registerReceiver(broadCastReceiver, intentFilter)
    }

    private fun retrieveHomeFromSP() {
        val json: String? = sp.getString("HomeLocation", "")
        if (json != "") {
            tracker.currentLocation = Gson().fromJson(json, LocationInfo::class.java)
            sp.edit().putString("current", Gson().toJson(tracker.currentLocation)).apply() // save to SP
            setHome.performClick()
        }
    }

    private fun setHomeVisibility(textVisibility: Int, buttonVisibility: Int) {
        homeTitle.visibility = textVisibility
        homeLocationInfo.visibility = textVisibility
        setHome.visibility = buttonVisibility
        clearHome.visibility = buttonVisibility
    }

    fun onLocationUpdate() {
        val currentLocationTitle: TextView = findViewById(R.id.currentLocation)
        currentLocationTitle.visibility = TextView.VISIBLE
        currentLocationInfo.visibility = TextView.VISIBLE
        currentLocationInfo.text = "Latitude: ${tracker.currentLocation?.latitude}\nLongitude: " +
                "${tracker.currentLocation?.longitude}\nAccuracy: ${tracker.currentLocation?.accuracy}"
        if (tracker.currentLocation?.accuracy!! < 50) {
            this.setHome.visibility = Button.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReceiver)
        tracker.fusedLocationClient.removeLocationUpdates(tracker.mLocationCallback)
    }

    ////////// permissions: //////////

    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        // checks if the user has turned on location from the setting (assuming user grant the app to use location)
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermissions(permission: String, permissionId: Int) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), permissionId)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID_LOCATION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                trackingButton.performClick()
            }
            else {
                Toast.makeText(applicationContext, "App can't operate without location permission", Toast.LENGTH_LONG).show()
            }
        }
        else if (requestCode == PERMISSION_ID_SMS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setPhoneButton.performClick()
            }
            else {
                Toast.makeText(applicationContext, "App can't operate without SMS permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    ////////// On-click implementations: //////////

    private fun onTrackingButton() {
        if (!checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, PERMISSION_ID_LOCATION)
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

    private fun onSetHome() {
        // assuming function was called when location accuracy is smaller than 50 meters
        val home = tracker.currentLocation ?: ""
        sp.edit().putString("HomeLocation", Gson().toJson(home)).apply() // save to SP
        homeLocationInfo.text = "Latitude: ${tracker.currentLocation?.latitude}\nLongitude: ${tracker.currentLocation?.longitude}"
        setHomeVisibility(TextView.VISIBLE, Button.VISIBLE)
        setHome.visibility = Button.GONE
    }

    private fun onClearHome() {
        sp.edit().remove("HomeLocation").apply()
        setHomeVisibility(TextView.GONE, Button.GONE)
    }

    private fun onSetPhoneButton() {
        if (!checkPermission(Manifest.permission.SEND_SMS)) {
            requestPermissions(Manifest.permission.SEND_SMS, PERMISSION_ID_SMS)
        } else {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Insert phone Number")
            alertDialog.setMessage("Phone Number: ")
            val input = EditText(this)
            input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            alertDialog.setView(input)
            alertDialog.setPositiveButton("Insert") { _, _ ->
                val phoneNumber = input.text.toString()
                sp.edit().putString("phoneNumber", Gson().toJson(phoneNumber)).apply() // save to SP
                testSms.visibility = if (phoneNumber.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            }
            alertDialog.show()
        }
    }

    private fun onTestSms() {
        val intent = Intent()
        intent.action = "POST_PC.ACTION_SEND_SMS"
        intent.putExtra("PHONE", sp.getString("phoneNumber", ""))
        intent.putExtra("CONTENT", "Honey I'm Sending a Test Message!")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

}
