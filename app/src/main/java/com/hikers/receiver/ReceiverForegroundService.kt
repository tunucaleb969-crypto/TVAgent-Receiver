package com.hikers.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReceiverForegroundService : Service() {

    private val TAG = "ReceiverService"
    private val DEVICE_ID = "hikers-tv"

    private lateinit var database: FirebaseDatabase
    private var lastProcessedTimestamp: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var disconnectedSince: Long = 0L
    private val watchdogIntervalMs = 10_000L

    private val commandListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val action = snapshot.child("action").getValue(String::class.java) ?: return
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

            if (timestamp <= lastProcessedTimestamp) return
            lastProcessedTimestamp = timestamp

            Log.d(TAG, "Executing command: $action")
            RemoteAccessibilityService.instance?.performCommand(action)

            writeStatus(action, timestamp)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "Command listener cancelled: ${error.message}")
        }
    }

    private val connectionListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val connected = snapshot.getValue(Boolean::class.java) ?: false
            if (connected) {
                disconnectedSince = 0L
                Log.d(TAG, "Firebase connected")
            } else {
                if (disconnectedSince == 0L) disconnectedSince = System.currentTimeMillis()
                Log.d(TAG, "Firebase disconnected")
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (disconnectedSince != 0L &&
                System.currentTimeMillis() - disconnectedSince > 30_000L
            ) {
                Log.w(TAG, "Disconnected >30s, forcing reconnect")
                database.goOffline()
                database.goOnline()
                disconnectedSince = 0L
            }
            handler.postDelayed(this, watchdogIntervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()

        database = FirebaseDatabase.getInstance()
        database.getReference("remote/$DEVICE_ID/command")
            .addValueEventListener(commandListener)
        database.getReference(".info/connected")
            .addValueEventListener(connectionListener)

        handler.postDelayed(watchdogRunnable, watchdogIntervalMs)
    }

    private fun writeStatus(lastAction: String, timestamp: Long) {
        val statusRef = database.getReference("remote/$DEVICE_ID/status")
        statusRef.child("lastAction").setValue(lastAction)
        statusRef.child("lastAckTimestamp").setValue(timestamp)
    }

    private fun startForegroundWithNotification() {
        val channelId = "receiver_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TVAgent Receiver",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("TVAgent-Receiver")
            .setContentText("Listening for remote commands")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(watchdogRunnable)
        database.getReference("remote/$DEVICE_ID/command").removeEventListener(commandListener)
        database.getReference(".info/connected").removeEventListener(connectionListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
