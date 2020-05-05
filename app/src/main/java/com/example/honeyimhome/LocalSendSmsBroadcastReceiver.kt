package com.example.honeyimhome

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class LocalSendSmsBroadcastReceiver: BroadcastReceiver() {

    private val PHONE: String = "PHONE"
    private val CONTENT: String = "CONTENT"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (!checkPermission(context!!)) {
            Log.i("LocalSendSms_onReceive", "Missing SEND_SMS permission")
            return
        }
        val phone = intent!!.getStringExtra(PHONE)
        val content = intent.getStringExtra(CONTENT)
        if (phone.isNullOrEmpty() || content.isNullOrEmpty()) {
            Log.i("LocalSendSms_onReceive", "Error extracting data from the intent's extras")
            return
        }
        SmsManager.getDefault().sendTextMessage(phone, null, content, null, null)
        pushNotification(context, phone, content)
    }

    private fun checkPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun pushNotification(context: Context, phone: String, message: String) {
        val notification = "sending sms to $phone: $message"
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, "id")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Honey I'm Home")
            .setContentText(notification)
        NotificationManagerCompat.from(context).notify(5, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("id", "name", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

}