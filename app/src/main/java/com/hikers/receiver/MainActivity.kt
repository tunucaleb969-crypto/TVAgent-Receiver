package com.hikers.receiver

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {

    private val PREFS = "crash_prefs"
    private val KEY_LAST_CRASH = "last_crash"

    override fun onCreate(savedInstanceState: Bundle?) {
        installCrashHandler()
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val statusText = TextView(this).apply {
            text = "TVAgent-Receiver\n\nEnable Accessibility Service, then start the Receiver Service."
            textSize = 16f
        }
        layout.addView(statusText)

        layout.addView(Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        layout.addView(Button(this).apply {
            text = "Start Receiver Service"
            setOnClickListener {
                val serviceIntent = Intent(this@MainActivity, ReceiverForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "View Last Crash"
            setOnClickListener { showLastCrash() }
        })

        setContentView(layout)
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CRASH, sw.toString())
                .apply()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun showLastCrash() {
        val crash = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CRASH, "No crash recorded yet.")
        AlertDialog.Builder(this)
            .setTitle("Last Crash")
            .setMessage(crash)
            .setPositiveButton("OK", null)
            .show()
    }
}
