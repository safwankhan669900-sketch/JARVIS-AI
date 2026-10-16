package com.example.jarvis

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false
    private var readyToListen = false

    private var updateStatus: ((String) -> Unit)? = null
    private var speakAgain: (() -> Unit)? = null

    private val microphonePermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                updateStatus?.invoke("MICROPHONE PERMISSION DENIED")
                speak("Boss, please allow microphone permission.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        updateStatus?.invoke("LISTENING...")
                    }

                    override fun onBeginningOfSpeech() {
                        updateStatus?.invoke("HEARING YOUR VOICE...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        updateStatus?.invoke("PROCESSING...")
                    }

                    override fun onError(error: Int) {
                        listening = false
                        updateStatus?.invoke("TAP JARVIS TO TRY AGAIN")
                    }

                    override fun onResults(results: Bundle?) {
                        listening = false

                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val command = matches?.firstOrNull().orEmpty()

                        if (command.isNotBlank()) {
                            handleCommand(command)
                        } else {
                            updateStatus?.invoke("NO COMMAND HEARD")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
            )
        }

        setContent {
            JarvisScreen(
                onStatus = { updateStatus = it },
                onListen = { startListening() }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            readyToListen = true
        }
    }

    private fun speak(message: String) {
        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_response"
        )
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateStatus?.invoke("SPEECH RECOGNITION UNAVAILABLE")
            Toast.makeText(
                this,
                "Speech recognition is not available on this phone.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (listening) return

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak to JARVIS"
                )
            }

            listening = true
            updateStatus?.invoke("STARTING MICROPHONE...")
            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            listening = false
            updateStatus?.invoke("COULD NOT START MICROPHONE")
        }
    }

    private fun handleCommand(rawCommand: String) {
        val command = rawCommand.lowercase(Locale.US)
            .replace("hey jarvis", "")
            .replace("jarvis", "")
            .trim()

        updateStatus?.invoke("COMMAND: $rawCommand")

        when {
            command.contains("youtube") -> {
                speak("Opening YouTube, boss.")
                openApp(
                    "com.google.android.youtube",
                    "https://www.youtube.com"
                )
            }

            command.contains("instagram") -> {
                speak("Opening Instagram, boss.")
                openApp(
                    "com.instagram.android",
                    "https://www.instagram.com"
                )
            }

            command.contains("time") -> {
                val time = SimpleDateFormat(
                    "h:mm a",
                    Locale.US
                ).format(Date())

                speak("The time is $time, boss.")
            }

            command.contains("hello") || command.contains("hi") -> {
                speak("Hello, boss. I am listening.")
            }

            command.contains("stop") -> {
                speak("Okay, boss.")
                updateStatus?.invoke("STANDING BY")
                return
            }

            else -> {
                speak("Sorry, boss. I do not know that command yet.")
            }
        }

        updateStatus?.invoke("TAP JARVIS FOR ANOTHER COMMAND")
    }

    private fun openApp(packageName: String, fallbackUrl: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
            }
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
        } catch (e: Exception) {
            speak("Sorry, boss. I could not open that application.")
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun JarvisScreen(
    onStatus: (((String) -> Unit) -> Unit),
    onListen: () -> Unit
) {
    var status by remember {
        mutableStateOf("SYSTEM ONLINE")
    }

    SideEffect {
        onStatus { newStatus -> status = newStatus }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050B14))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "J.A.R.V.I.S",
            color = Color(0xFF00E5FF),
            fontSize = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = status,
            color = Color.White,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onListen,
            modifier = Modifier.size(170.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF063B50)
            )
        ) {
            Text(
                text = "JARVIS",
                color = Color(0xFF00E5FF),
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "VOICE ASSISTANT",
            color = Color(0xFF00E5FF),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap and speak a command.",
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}