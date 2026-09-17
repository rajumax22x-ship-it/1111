package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatMessageEntity
import com.example.data.CodeSnippetEntity
import com.example.data.JarvisDatabase
import com.example.network.Content
import com.example.network.GeminiRequest
import com.example.network.InlineData
import com.example.network.JarvisNetwork
import com.example.network.Part
import com.example.network.SystemInstruction
import com.example.utils.JarvisCommandExecutor
import com.example.utils.JarvisVoiceManager
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val database = JarvisDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    private val snippetDao = database.codeSnippetDao()

    val chatMessages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codeSnippets: StateFlow<List<CodeSnippetEntity>> = snippetDao.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _elevenLabsKey = MutableStateFlow("")
    val elevenLabsKey: StateFlow<String> = _elevenLabsKey.asStateFlow()

    private val _elevenLabsVoiceId = MutableStateFlow("21m00Tcm4TlvDq8ikWAM")
    val elevenLabsVoiceId: StateFlow<String> = _elevenLabsVoiceId.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _micSensitivity = MutableStateFlow(2.5f)
    val micSensitivity: StateFlow<Float> = _micSensitivity.asStateFlow()

    private val _wakeWord = MutableStateFlow("FRIDAY")
    val wakeWord: StateFlow<String> = _wakeWord.asStateFlow()

    private val _selectedModel = MutableStateFlow("Gemini (Flash)")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _openAiKey = MutableStateFlow("")
    val openAiKey: StateFlow<String> = _openAiKey.asStateFlow()

    val voiceManager = JarvisVoiceManager(application)

    fun setElevenLabsConfig(key: String, voiceId: String) {
        _elevenLabsKey.value = key
        _elevenLabsVoiceId.value = voiceId
    }

    fun setMicSensitivity(sensitivity: Float) {
        _micSensitivity.value = sensitivity
    }

    fun setWakeWord(word: String) {
        _wakeWord.value = word
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun setOpenAiKey(key: String) {
        _openAiKey.value = key
    }

    fun toggleVoice() {
        _isVoiceEnabled.update { !it }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            // Save user message
            chatDao.insertMessage(ChatMessageEntity(sender = "user", message = prompt))
            _isLoading.value = true

            // Check if prompt is a direct JSON action intent from user/system
            if (prompt.trim().startsWith("{") && prompt.contains("action")) {
                val result = JarvisCommandExecutor.executeJsonAction(getApplication(), prompt)
                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = result.message))
                _isLoading.value = false
                if (_isVoiceEnabled.value) {
                    voiceManager.speak(result.message, _elevenLabsKey.value, _elevenLabsVoiceId.value)
                }
                return@launch
            }

            // Check quick commands first
            val quickResponse = JarvisCommandExecutor.parseAndExecuteQuickCommand(getApplication(), prompt)
            if (quickResponse != null) {
                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = quickResponse))
                _isLoading.value = false
                if (_isVoiceEnabled.value) {
                    voiceManager.speak(quickResponse, _elevenLabsKey.value, _elevenLabsVoiceId.value)
                }
                return@launch
            }

            // Call Gemini API
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = "API key missing, Sir. Please configure your Gemini API key in the Secrets panel or Settings."
                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = errorMsg))
                _isLoading.value = false
                return@launch
            }

            val systemPrompt = """
                Role: You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the world's most advanced, witty, tactical, and deeply intelligent AI companion and operational assistant created by Tony Stark. You address the user respectfully as 'Sir', 'Boss', or 'सर'.
                Deep Intelligence & Language Comprehension ("हमारी बात समझ सके"):
                * Native Multilingual Fluency: You possess complete, native, fluent understanding of Hindi (शुद्ध और बोलचाल की हिंदी), Hinglish (e.g., "bhai suno", "yeh batao", "mere samne kya hai", "kya haal hai", "kuch madad karo"), and English.
                * Conversational Matching: If the user speaks or writes in Hindi or Hinglish, reply with courteous, natural, intelligent Hindi/Hinglish (e.g. "नमस्ते सर, मैं आपकी सेवा में हाजिर हूँ।", "बिल्कुल सर, मैं समझ गया।"). If the user uses English, reply in refined, witty British English.
                * Visual Perception ("हमको देख सके"): You possess high-tech optical sensors and camera vision. When provided with an image or camera snapshot, inspect every detail with precision. Recognize faces, people, expressions, objects, rooms, surroundings, or text. Confirm: "Yes Sir, I can see you clearly" or "हाँ सर, मैं आपको देख सकता हूँ!".
                * Thinking & Reasoning ("सोच समझ सके"): Think deeply and logically. Provide concise, smart solutions, anticipate the user's needs, and assist with any question or task with supreme intelligence.
                * Device Actions: When asked to open apps, check battery, open settings, set alarms, or control features, output a conversational confirmation and structured JSON:
                { "action": "OPEN_APP", "package_name": "com.whatsapp" } or { "action": "CHECK_BATTERY" }
            """.trimIndent()

            val currentModel = _selectedModel.value
            try {
                val reply: String = if (currentModel.contains("ChatGPT", ignoreCase = true)) {
                    val oKey = _openAiKey.value.ifBlank { "sk-demo-key" }
                    if (oKey == "sk-demo-key" || oKey.isBlank()) {
                        "OpenAI API Key is missing, Sir. Please enter your ChatGPT / OpenAI API key in Settings."
                    } else {
                        val messagesList = mutableListOf<com.example.network.OpenAiMessage>()
                        messagesList.add(com.example.network.OpenAiMessage(role = "system", content = systemPrompt))
                        val recentMsgs = chatMessages.value.takeLast(6)
                        for (m in recentMsgs) {
                            messagesList.add(com.example.network.OpenAiMessage(role = if (m.sender == "user") "user" else "assistant", content = m.message))
                        }
                        messagesList.add(com.example.network.OpenAiMessage(role = "user", content = prompt))
                        val req = com.example.network.OpenAiRequest(
                            model = "gpt-4o",
                            messages = messagesList
                        )
                        val res = com.example.network.JarvisNetwork.openAiService.chatCompletions("Bearer $oKey", req)
                        res.choices?.firstOrNull()?.message?.content ?: "Empty neural response from ChatGPT, Sir."
                    }
                } else {
                    // Gemini 3.5 Flash
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                        "Gemini API key missing, Sir. Please configure your API key in the Secrets panel or Settings."
                    } else {
                        // Multi-turn conversational memory
                        val recentMsgs = chatMessages.value.takeLast(8)
                        val contentsList = mutableListOf<Content>()
                        for (m in recentMsgs) {
                            if (m.message.isNotBlank()) {
                                contentsList.add(
                                    Content(
                                        role = if (m.sender == "user") "user" else "model",
                                        parts = listOf(Part(text = m.message))
                                    )
                                )
                            }
                        }
                        if (contentsList.isEmpty() || contentsList.lastOrNull()?.parts?.firstOrNull()?.text != prompt) {
                            contentsList.add(
                                Content(
                                    role = "user",
                                    parts = listOf(Part(text = prompt))
                                )
                            )
                        }

                        val request = GeminiRequest(
                            contents = contentsList,
                            systemInstruction = SystemInstruction(parts = listOf(Part(text = systemPrompt)))
                        )

                        val response = JarvisNetwork.retrofitService.generateContent(apiKey, request)
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                            ?: "I encountered a slight neural glitch, Sir. Please repeat your query."
                    }
                }

                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = reply))

                // Check for JSON action payload in reply
                if (reply.contains("{") && reply.contains("action")) {
                    try {
                        val jsonStartIndex = reply.indexOf("{")
                        val jsonEndIndex = reply.lastIndexOf("}") + 1
                        if (jsonStartIndex >= 0 && jsonEndIndex > jsonStartIndex) {
                            val jsonString = reply.substring(jsonStartIndex, jsonEndIndex)
                            JarvisCommandExecutor.executeJsonAction(getApplication(), jsonString)
                        }
                    } catch (e: Exception) {
                        // ignore JSON parse errors in conversational reply
                    }
                }

                // Check for code blocks and extract them into snippets
                if (reply.contains("```")) {
                    extractAndSaveCodeSnippet(reply)
                }

                if (_isVoiceEnabled.value) {
                    val speechText = reply.replace(Regex("```[\\s\\S]*?```"), "Code snippet provided, Sir.")
                        .replace(Regex("[#*_`[-]"), "")
                    voiceManager.speak(speechText, _elevenLabsKey.value, _elevenLabsVoiceId.value)
                }
            } catch (e: Exception) {
                val errorMsg = if (e.message?.contains("resource_exhausted", ignoreCase = true) == true || 
                                   e.message?.contains("quota", ignoreCase = true) == true) {
                    "It appears our API quota has been thoroughly exhausted, Sir. Even Stark Industries has bandwidth limits. Please check your Google AI Studio billing details or switch to ChatGPT in Settings."
                } else {
                    "Network telemetry failure, Sir: ${e.message}"
                }
                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = errorMsg))
                if (_isVoiceEnabled.value) {
                    voiceManager.speak("Our API quota has been exhausted, Sir. Please check your billing details or switch models.", _elevenLabsKey.value, _elevenLabsVoiceId.value)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendVisionPrompt(prompt: String, bitmap: Bitmap) {
        viewModelScope.launch {
            val userText = if (prompt.isNotBlank()) prompt else "Please scan and analyze this visual optical feed, Sir. Tell me who and what you see in front of you."
            chatDao.insertMessage(ChatMessageEntity(sender = "user", message = "📷 [Optical Vision Scan]: $userText"))
            _isLoading.value = true

            try {
                // Compress bitmap to base64 JPEG
                val outputStream = ByteArrayOutputStream()
                val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                    val ratio = Math.min(1024f / bitmap.width, 1024f / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else {
                    bitmap
                }
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    val errorMsg = "API key missing, Sir. Please configure your Gemini API key in the Secrets panel or Settings."
                    chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = errorMsg))
                    _isLoading.value = false
                    return@launch
                }

                val systemPrompt = """
                    Role: You are J.A.R.V.I.S., Tony Stark's brilliant AI with active optical vision sensors ("Stark Optical Vision"). Address the user as 'Sir', 'Boss', or 'सर'.
                    Multilingual Understanding: Fluent in Hindi, Hinglish, and English.
                    Visual Perception ("हमको देख सके"): Inspect this captured image thoroughly.
                    - If a person / the user is in the image, acknowledge seeing them warmly and describe their expression, posture, clothing, surroundings, or what they are doing (e.g. "Yes Sir, I can see you clearly!", "हाँ सर, मैं आपको देख पा रहा हूँ!").
                    - If objects, text, screens, or documents are present, identify and explain them clearly.
                    - Keep the explanation crisp, sharp, intelligent, and charismatic.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = listOf(
                                Part(text = userText),
                                Part(inline_data = InlineData(mime_type = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    systemInstruction = SystemInstruction(parts = listOf(Part(text = systemPrompt)))
                )

                val response = JarvisNetwork.retrofitService.generateContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Visual sensors processed the image, Sir, but telemetry was inconclusive."

                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = reply))

                if (_isVoiceEnabled.value) {
                    val speechText = reply.replace(Regex("[#*_`[-]"), "")
                    voiceManager.speak(speechText, _elevenLabsKey.value, _elevenLabsVoiceId.value)
                }
            } catch (e: Exception) {
                val errorMsg = "Optical telemetry failure, Sir: ${e.message}"
                chatDao.insertMessage(ChatMessageEntity(sender = "jarvis", message = errorMsg))
                if (_isVoiceEnabled.value) {
                    voiceManager.speak("Optical sensor analysis encountered an anomaly, Sir.", _elevenLabsKey.value, _elevenLabsVoiceId.value)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractAndSaveCodeSnippet(reply: String) {
        viewModelScope.launch {
            try {
                val regex = Regex("```([a-zA-Z]*)\\n([\\s\\S]*?)```")
                val matches = regex.findAll(reply)
                for (match in matches) {
                    val lang = match.groups[1]?.value?.ifBlank { "kotlin" } ?: "kotlin"
                    val code = match.groups[2]?.value?.trim() ?: continue
                    val titleHeader = "Generated Snippet (${lang.uppercase()})"
                    snippetDao.insertSnippet(CodeSnippetEntity(title = titleHeader, language = lang, code = code))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun saveSnippet(title: String, language: String, code: String) {
        viewModelScope.launch {
            snippetDao.insertSnippet(CodeSnippetEntity(title = title, language = language, code = code))
        }
    }

    fun deleteSnippet(id: Long) {
        viewModelScope.launch {
            snippetDao.deleteSnippet(id)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatDao.clearMessages()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
