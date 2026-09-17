package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class JarvisAudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File? {
        try {
            outputFile = File.createTempFile("jarvis_voice_cmd_", ".m4a", context.cacheDir)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            Log.d("JarvisAudioRecorder", "Started recording to: ${outputFile?.absolutePath}")
            return outputFile
        } catch (e: IOException) {
            Log.e("JarvisAudioRecorder", "prepare() failed: ${e.message}")
        } catch (e: Exception) {
            Log.e("JarvisAudioRecorder", "startRecording failed: ${e.message}")
        }
        return null
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("JarvisAudioRecorder", "stopRecording failed: ${e.message}")
        } finally {
            mediaRecorder = null
        }
        return outputFile
    }
}
