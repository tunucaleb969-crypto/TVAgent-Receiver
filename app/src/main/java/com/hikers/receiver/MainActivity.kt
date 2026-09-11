package com.hikers.receiver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val statusText = TextView(this).apply {
            text = "TVAgent-Receiver\n\nThis app runs in the background to receive remote control commands.\n\nMake sure to enable its Accessibility Service below."
            textSize = 16f
        }
        layout.addView(statusText)

        val openAccessibilityButton = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(openAccessibilityButton)

        val startServiceButton = Button(this).apply {
            text = "Start Receiver Service"
            setOnClickListener {
                val serviceIntent = Intent(this@MainActivity, ReceiverForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
        layout.addView(startServiceButton)

        setContentView(layout)
    }
}
