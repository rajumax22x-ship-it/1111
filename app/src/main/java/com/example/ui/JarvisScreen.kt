package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessageEntity
import com.example.data.CodeSnippetEntity
import com.example.ui.theme.*
import com.example.utils.JarvisAudioRecorder
import com.example.utils.JarvisCommandExecutor
import com.example.viewmodel.JarvisViewModel
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainScreen(viewModel: JarvisViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FRIDAY AI SYSTEM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = JarvisCyan,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "STARK INDUSTRIES // ONLINE",
                                fontSize = 10.sp,
                                color = JarvisGold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleVoice() }) {
                        Icon(
                            imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Voice",
                            tint = if (isVoiceEnabled) JarvisCyan else JarvisTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JarvisSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "HUD") },
                    label = { Text("HUD Chat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary,
                        indicatorColor = JarvisSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Commands") },
                    label = { Text("Commands") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary,
                        indicatorColor = JarvisSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Code Studio") },
                    label = { Text("Code Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary,
                        indicatorColor = JarvisSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary,
                        indicatorColor = JarvisSurfaceVariant
                    )
                )
            }
        },
        containerColor = JarvisDarkBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> JarvisChatTab(viewModel, isLoading)
                1 -> JarvisCommandsTab(context)
                2 -> JarvisCodeStudioTab(viewModel)
                3 -> JarvisSettingsTab(viewModel)
            }
        }
    }
}

@Composable
fun JarvisChatTab(viewModel: JarvisViewModel, isLoading: Boolean) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val audioRecorder = remember { JarvisAudioRecorder(context) }
    var isRecordingAudio by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = audioRecorder.startRecording()
            if (file != null) {
                isRecordingAudio = true
                Toast.makeText(context, "MediaRecorder recording started, Sir.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to start MediaRecorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for MediaRecorder recording, Sir.", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val prompt = if (inputText.isNotBlank()) inputText else "Please scan and analyze this visual optical feed. Describe who and what you see in front of you, Sir."
            viewModel.sendVisionPrompt(prompt, bitmap)
            inputText = ""
            Toast.makeText(context, "Analyzing optical camera feed with Jarvis Vision...", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission required for Jarvis Vision, Sir.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerVisionCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.sendMessage(spokenText)
            }
        }
    }

    fun triggerSpeechRecognition() {
        com.example.utils.JarvisSoundPlayer.playStartBeep()
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "hi-IN"))
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "बोलिए सर, मैं सुन रहा हूँ... (Speak your command, Sir)")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSurface.copy(alpha = 0.5f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { triggerVisionCapture() },
                label = { Text("👁️ Vision (See Me / मुझे देखो)", fontSize = 11.sp, color = JarvisCyan, fontWeight = FontWeight.Bold) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceVariant)
            )
            AssistChip(
                onClick = { triggerSpeechRecognition() },
                label = { Text("🎙️ बात करो (Talk)", fontSize = 11.sp, color = JarvisGold, fontWeight = FontWeight.Bold) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceVariant)
            )
            AssistChip(
                onClick = { viewModel.sendMessage("क्या तुम मुझे देख सकते हो और सुन सकते हो?") },
                label = { Text("क्या तुम मुझे देख सकते हो?", fontSize = 11.sp, color = JarvisCyan) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceVariant)
            )
            AssistChip(
                onClick = { viewModel.sendMessage("Jarvis, open YouTube") },
                label = { Text("Open YouTube", fontSize = 11.sp, color = JarvisCyan) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceVariant)
            )
            AssistChip(
                onClick = { viewModel.sendMessage("Check device battery status") },
                label = { Text("Battery", fontSize = 11.sp, color = JarvisCyan) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceVariant)
            )
            AssistChip(
                onClick = { viewModel.sendMessage("Build a clean Kotlin Jetpack Compose counter app code") },
                label = { Text("Compose App Code", fontSize = 11.sp, color = JarvisGold) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceVariant)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                JarvisAudioVisualizer(isListening = true)
            }
        }

        if (isRecordingAudio) {
            Surface(
                color = Color.Red.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.Red)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MediaRecorder Voice Recording Active... Tap Stop to submit command.", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "At your service, Sir. How may I assist?",
                                color = JarvisTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = JarvisCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JARVIS is computing...",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Surface(
            color = JarvisSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("बात करें या निर्देश दें (Ask or command)...", color = JarvisTextSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        cursorColor = JarvisCyan
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { triggerVisionCapture() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(JarvisSurfaceVariant, RoundedCornerShape(21.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Jarvis Vision Scan",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { triggerSpeechRecognition() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(JarvisSurfaceVariant, RoundedCornerShape(21.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = JarvisGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (isRecordingAudio) {
                            val recordedFile = audioRecorder.stopRecording()
                            isRecordingAudio = false
                            if (recordedFile != null) {
                                val sizeKb = recordedFile.length() / 1024
                                viewModel.sendMessage("Voice command audio recorded via MediaRecorder (${sizeKb} KB). Please process command.")
                                Toast.makeText(context, "Voice command recorded & saved", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                val file = audioRecorder.startRecording()
                                if (file != null) {
                                    isRecordingAudio = true
                                    Toast.makeText(context, "MediaRecorder recording started, Sir.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (isRecordingAudio) Color.Red else JarvisSurfaceVariant, RoundedCornerShape(21.dp))
                ) {
                    Icon(
                        imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.FiberSmartRecord,
                        contentDescription = if (isRecordingAudio) "Stop MediaRecorder" else "MediaRecorder Record",
                        tint = if (isRecordingAudio) Color.White else JarvisTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(JarvisCyan, RoundedCornerShape(21.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessageEntity) {
    val isUser = msg.sender == "user"
    val bubbleColor = if (isUser) JarvisSurfaceVariant else JarvisSurface
    val borderColor = if (isUser) JarvisGold.copy(alpha = 0.5f) else JarvisCyan.copy(alpha = 0.5f)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = if (isUser) JarvisGold else JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isUser) "Sir / Boss" else "FRIDAY",
                    color = if (isUser) JarvisGold else JarvisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = msg.message,
                color = JarvisTextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun JarvisCommandsTab(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM COMMAND CONTROL",
            color = JarvisCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Execute device actions directly through Jarvis telemetry protocols.",
            color = JarvisTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CommandCard(
                title = "YouTube",
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.weight(1f)
            ) {
                JarvisCommandExecutor.parseAndExecuteQuickCommand(context, "youtube")
                Toast.makeText(context, "Opening YouTube", Toast.LENGTH_SHORT).show()
            }
            CommandCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f)
            ) {
                JarvisCommandExecutor.parseAndExecuteQuickCommand(context, "settings")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CommandCard(
                title = "Battery Check",
                icon = Icons.Default.BatteryChargingFull,
                modifier = Modifier.weight(1f)
            ) {
                val res = JarvisCommandExecutor.parseAndExecuteQuickCommand(context, "battery")
                Toast.makeText(context, res, Toast.LENGTH_LONG).show()
            }
            CommandCard(
                title = "Optic Camera",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f)
            ) {
                JarvisCommandExecutor.parseAndExecuteQuickCommand(context, "camera")
            }
        }
    }
}

@Composable
fun CommandCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = JarvisTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun JarvisCodeStudioTab(viewModel: JarvisViewModel) {
    val snippets by viewModel.codeSnippets.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var customPrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SOFTWARE DEVELOPMENT STUDIO",
            color = JarvisGold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Request full-stack apps, React, Flutter, Python, or Kotlin code architectures.",
            color = JarvisTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = customPrompt,
            onValueChange = { customPrompt = it },
            placeholder = { Text("e.g. Build an E-commerce Flutter app structure", color = JarvisTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisGold,
                unfocusedBorderColor = JarvisGold.copy(alpha = 0.3f),
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (customPrompt.isNotBlank()) {
                    viewModel.sendMessage("Build the following software architecture: $customPrompt")
                    customPrompt = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = JarvisGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Code, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Code Architecture", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SAVED CODE ARCHITECTURES",
            color = JarvisCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(snippets) { snippet ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisSurface),
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = snippet.title, color = JarvisCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis Code", snippet.code))
                                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = JarvisGold, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteSnippet(snippet.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = JarvisSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = snippet.code,
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JarvisSettingsTab(viewModel: JarvisViewModel) {
    var elevenKey by remember { mutableStateOf("") }
    var voiceId by remember { mutableStateOf("21m00Tcm4TlvDq8ikWAM") }

    val micSensitivity by viewModel.micSensitivity.collectAsStateWithLifecycle()
    val wakeWordState by viewModel.wakeWord.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()

    var sensitivitySlider by remember { mutableFloatStateOf(micSensitivity) }
    var wakeWordInput by remember { mutableStateOf(wakeWordState) }
    var openAiKeyInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI ENGINE & CONFIGURATION",
            color = JarvisCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Select active AI Model (Gemini, Gemini Studio, or ChatGPT) and configure parameters.",
            color = JarvisTextSecondary,
            fontSize = 12.sp
        )

        // Model Selector
        Text(text = "Active AI Model", color = JarvisGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val models = listOf("Gemini (Flash)", "Gemini Studio", "ChatGPT")
            models.forEach { model ->
                val isSelected = selectedModel == model
                Button(
                    onClick = { viewModel.setSelectedModel(model) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) JarvisCyan else JarvisSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model,
                        color = if (isSelected) Color.Black else JarvisTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (selectedModel == "ChatGPT") {
            OutlinedTextField(
                value = openAiKeyInput,
                onValueChange = { openAiKeyInput = it },
                label = { Text("OpenAI / ChatGPT API Key (sk-...)", color = JarvisTextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                )
            )
        }

        OutlinedTextField(
            value = wakeWordInput,
            onValueChange = { wakeWordInput = it },
            label = { Text("Trigger Word (Wake Word)", color = JarvisTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            )
        )

        Column {
            Text(
                text = "Microphone Sensitivity: ${String.format(java.util.Locale.getDefault(), "%.1f", sensitivitySlider)}x",
                color = JarvisTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = sensitivitySlider,
                onValueChange = { sensitivitySlider = it },
                valueRange = 1.0f..5.0f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan
                )
            )
        }

        OutlinedTextField(
            value = elevenKey,
            onValueChange = { elevenKey = it },
            label = { Text("ElevenLabs API Key (Optional)", color = JarvisTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            )
        )

        OutlinedTextField(
            value = voiceId,
            onValueChange = { voiceId = it },
            label = { Text("ElevenLabs Voice ID", color = JarvisTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisCyan.copy(alpha = 0.3f),
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            )
        )

        Button(
            onClick = {
                viewModel.setElevenLabsConfig(elevenKey, voiceId)
                viewModel.setMicSensitivity(sensitivitySlider)
                viewModel.setWakeWord(wakeWordInput)
                if (openAiKeyInput.isNotBlank()) {
                    viewModel.setOpenAiKey(openAiKeyInput)
                }
                Toast.makeText(context, "Configurations updated successfully, Sir.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Configurations", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Toast.makeText(context, "All permissions granted, Sir.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Some permissions were denied.", Toast.LENGTH_SHORT).show()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val perms = mutableListOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grant Permissions", color = JarvisCyan, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    com.example.utils.JarvisNotificationHelper.showJarvisNotification(
                        context,
                        "J.A.R.V.I.S. Alert",
                        "All systems online, Sir. Surveillance and notifications active."
                    )
                    Toast.makeText(context, "Notification dispatched, Sir.", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Notification", color = JarvisGold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = BorderStroke(1.dp, JarvisGold.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Persona Status & Hardware", color = JarvisGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("AI Engine: Gemini 3.5 Flash", color = JarvisTextPrimary, fontSize = 12.sp)
                Text("Persona: Tony Stark's FRIDAY / JARVIS", color = JarvisTextPrimary, fontSize = 12.sp)
                Text("Wake Word: $wakeWordInput (Sensitivity: ${String.format(java.util.Locale.getDefault(), "%.1f", sensitivitySlider)}x)", color = JarvisTextPrimary, fontSize = 12.sp)
                Text("Camera & Notifications: Active / Ready", color = JarvisTextPrimary, fontSize = 12.sp)
                Text("Voice Telemetry: Active (ElevenLabs)", color = JarvisTextPrimary, fontSize = 12.sp)
            }
        }
    }
}
