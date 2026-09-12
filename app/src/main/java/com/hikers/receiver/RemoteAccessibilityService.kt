package com.hikers.receiver

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RemoteAccessibilityService : AccessibilityService() {

    private val TAG = "RemoteA11yService"
    private lateinit var audioManager: AudioManager

    companion object {
        var instance: RemoteAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d(TAG, "Accessibility service connected")
    }

    fun performCommand(action: String) {
        Log.d(TAG, "performCommand: $action")
        when (action) {
            "POWER" -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "DPAD_UP" -> performGlobalAction(GLOBAL_ACTION_DPAD_UP)
            "DPAD_DOWN" -> performGlobalAction(GLOBAL_ACTION_DPAD_DOWN)
            "DPAD_LEFT" -> performGlobalAction(GLOBAL_ACTION_DPAD_LEFT)
            "DPAD_RIGHT" -> performGlobalAction(GLOBAL_ACTION_DPAD_RIGHT)
            "DPAD_CENTER" -> performGlobalAction(GLOBAL_ACTION_DPAD_CENTER)
            "VOLUME_UP" -> audioManager.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_RAISE,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.FLAG_SHOW_UI
            )
            "VOLUME_DOWN" -> audioManager.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_LOWER,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.FLAG_SHOW_UI
            )
            else -> Log.w(TAG, "Unknown action: $action")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — this service only performs actions, doesn't react to events
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
