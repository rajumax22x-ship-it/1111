package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class JarvisVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK) // British accent for JARVIS!
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("JarvisVoice", "British English language not supported, defaulting to US English.")
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
        } else {
            Log.e("JarvisVoice", "TTS Initialization failed!")
        }
    }

    fun speak(text: String, elevenLabsKey: String = "", voiceId: String = "", onComplete: () -> Unit = {}) {
        if (elevenLabsKey.isNotBlank() && voiceId.isNotBlank()) {
            // Use ElevenLabs API
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = OkHttpClient()
                    val json = JSONObject().apply {
                        put("text", text)
                        put("model_id", "eleven_monolingual_v1")
                    }
                    val body = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                        .addHeader("xi-api-key", elevenLabsKey)
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "audio/mpeg")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            playAudioBytes(bytes, onComplete)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e("JarvisVoice", "ElevenLabs error: ${e.message}, falling back to local TTS")
                }
                speakLocal(text, onComplete)
            }
        } else {
            speakLocal(text, onComplete)
        }
    }

    private fun speakLocal(text: String, onComplete: () -> Unit) {
        if (isInitialized && tts != null) {
            JarvisSoundPlayer.playEndBeep()
            val containsHindi = text.any { it in '\u0900'..'\u097F' }
            if (containsHindi) {
                tts?.setLanguage(Locale("hi", "IN"))
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
            } else {
                val result = tts?.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setPitch(0.95f)
                tts?.setSpeechRate(1.0f)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JarvisUtteranceId")
            onComplete()
        } else {
            onComplete()
        }
    }

    private fun playAudioBytes(bytes: ByteArray, onComplete: () -> Unit) {
        try {
            JarvisSoundPlayer.playEndBeep()
            val tempFile = java.io.File.createTempFile("jarvis_audio", ".mp3", context.cacheDir)
            tempFile.writeBytes(bytes)
            val mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    release()
                    tempFile.delete()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e("JarvisVoice", "Audio playback error: ${e.message}")
            speakLocal("", onComplete)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
