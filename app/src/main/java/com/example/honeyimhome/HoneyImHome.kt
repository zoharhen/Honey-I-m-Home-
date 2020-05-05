package com.example.honeyimhome

import android.app.Application
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class HoneyImHome : Application() {
    override fun onCreate() {
        super.onCreate()

        val br = LocalSendSmsBroadcastReceiver()
        val filter = IntentFilter("POST_PC.ACTION_SEND_SMS")
        LocalBroadcastManager.getInstance(this).registerReceiver(br, filter)

        val work: PeriodicWorkRequest = PeriodicWorkRequestBuilder<HoneyImHomeWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(work)
    }
}