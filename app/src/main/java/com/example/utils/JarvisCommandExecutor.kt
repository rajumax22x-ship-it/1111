package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import org.json.JSONObject

object JarvisCommandExecutor {

    data class ExecutionResult(
        val message: String,
        val success: Boolean
    )

    fun executeJsonAction(context: Context, jsonString: String): ExecutionResult {
        return try {
            val json = JSONObject(jsonString)
            val action = json.optString("action")
            when (action) {
                "OPEN_APP" -> {
                    val packageName = json.optString("package_name")
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        context.startActivity(intent)
                        ExecutionResult("Opening application, Sir.", true)
                    } else {
                        // Fallback to searching intent or browser
                        ExecutionResult("Application not found on device, Boss.", false)
                    }
                }
                "CHECK_BATTERY" -> {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    ExecutionResult("Battery level is currently at $batLevel percent, Sir.", true)
                }
                "SET_ALARM" -> {
                    val message = json.optString("message", "JARVIS Alarm")
                    val hour = json.optInt("hour", 8)
                    val minute = json.optInt("minute", 0)
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ExecutionResult("Alarm set for $hour:$minute, Sir.", true)
                }
                "OPEN_SETTINGS" -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ExecutionResult("Opening system settings, Sir.", true)
                }
                "OPEN_URL" -> {
                    val url = json.optString("url")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ExecutionResult("Opening web link, Sir.", true)
                }
                else -> ExecutionResult("Unknown action received, Boss.", false)
            }
        } catch (e: Exception) {
            ExecutionResult("Executed action with minor anomaly: ${e.message}", false)
        }
    }

    fun parseAndExecuteQuickCommand(context: Context, query: String): String? {
        val lower = query.lowercase()
        return when {
            lower.contains("youtube") || lower.contains("यूट्यूब") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    context.startActivity(intent)
                    if (lower.any { it in '\u0900'..'\u097F' }) "यूट्यूब खोला जा रहा है, सर।" else "Opening YouTube for you, Sir."
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(webIntent)
                    if (lower.any { it in '\u0900'..'\u097F' }) "ब्राउज़र में यूट्यूब खोल रहा हूँ, सर।" else "Opening YouTube in browser, Sir."
                }
            }
            lower.contains("settings") || lower.contains("सेटिंग") -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                if (lower.any { it in '\u0900'..'\u097F' }) "सिस्टम सेटिंग्स खोली जा रही हैं, सर।" else "Opening system settings, Sir."
            }
            lower.contains("battery") || lower.contains("बैटरी") -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (lower.any { it in '\u0900'..'\u097F' }) "बैटरी वर्तमान में $batLevel प्रतिशत पर है, सर। सभी सिस्टम सुचारू हैं।" else "Battery is at $batLevel percent, Sir. All systems optimal."
            }
            lower.contains("camera") || lower.contains("कैमरा") -> {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                try {
                    context.startActivity(intent)
                    if (lower.any { it in '\u0900'..'\u097F' }) "कैमरा ऑप्टिक्स सक्रिय किया जा रहा है, सर।" else "Initializing camera optics, Sir."
                } catch (e: Exception) {
                    "Camera access unavailable, Boss."
                }
            }
            lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    context.startActivity(intent)
                    if (lower.any { it in '\u0900'..'\u097F' }) "व्हाट्सएप खोला जा रहा है, सर।" else "Launching WhatsApp, Sir."
                } else {
                    if (lower.any { it in '\u0900'..'\u097F' }) "व्हाट्सएप इस डिवाइस पर नहीं मिला, सर।" else "WhatsApp is not installed on this device, Sir."
                }
            }
            else -> null
        }
    }
}
