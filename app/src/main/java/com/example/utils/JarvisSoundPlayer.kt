package com.example.utils

import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Log

object JarvisSoundPlayer {
    fun playStartBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("JarvisSound", "Error playing start beep: ${e.message}")
        }
    }

    fun playEndBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) {
            Log.e("JarvisSound", "Error playing end beep: ${e.message}")
        }
    }
}
