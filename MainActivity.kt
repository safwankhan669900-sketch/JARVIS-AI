package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cyan = Color(0xFF25D9FF)
private val Blue = Color(0xFF187BFF)
private val Bg = Color(0xFF020812)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var updateStatus: ((String) -> Unit)? = null
    private var updateResponse: ((String) -> Unit)? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else {
            updateStatus?.invoke("MICROPHONE PERMISSION REQUIRED")
            speak("Microphone permission is required, sir.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            var status by remember { mutableStateOf("SYSTEM STANDBY") }
            var response by remember { mutableStateOf("At your service, sir.") }
            updateStatus = { status = it }
            updateResponse = { response = it }
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    HudScreen(status, response) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }
    }

    override fun onInit(result: Int) {
        ttsReady = result == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.UK
            tts?.setSpeechRate(1.03f)
        }
    }

    private fun speak(message: String) {
        if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateStatus?.invoke("SPEECH RECOGNITION UNAVAILABLE")
            speak("Speech recognition is unavailable on this device, sir.")
            return
        }
        updateStatus?.invoke("LISTENING...")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
        }
        try { startActivityForResult(intent, 201) } catch (_: Exception) {
            updateStatus?.invoke("VOICE INPUT FAILED")
            speak("I could not start voice input, sir.")
        }
    }

    @Deprecated("Compact starter uses Android's speech input activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 201) return
        val phrase = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (resultCode == RESULT_OK && !phrase.isNullOrBlank()) {
            updateResponse?.invoke("\"$phrase\"")
            command(phrase.lowercase(Locale.ROOT))
        } else updateStatus?.invoke("SYSTEM STANDBY")
    }

    private fun command(text: String) {
        when {
            text.contains("open youtube") -> launch("com.google.android.youtube", "Opening YouTube, sir.")
            text.contains("open instagram") -> launch("com.instagram.android", "Opening Instagram, sir.")
            text.contains("hello") || text.contains("hey jarvis") -> reply("Hello, sir. At your service.")
            text.contains("time") -> reply("The time is ${SimpleDateFormat("h:mm a", Locale.UK).format(Date())}, sir.")
            else -> reply("I heard you, sir. AI answers can be connected in a later version.")
        }
    }

    private fun launch(packageName: String, spoken: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) reply("That app does not appear to be installed, sir.")
        else { reply(spoken); startActivity(intent) }
    }

    private fun reply(message: String) {
        updateStatus?.invoke("RESPONDING")
        updateResponse?.invoke(message)
        speak(message)
        window.decorView.postDelayed({ updateStatus?.invoke("SYSTEM STANDBY") }, 1800)
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); super.onDestroy()
    }
}

@Composable
private fun HudScreen(status: String, response: String, onMic: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hud")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart), label = "rotation")
    val pulse by transition.animateFloat(0.92f, 1.04f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J.A.R.V.I.S", color = Cyan, fontSize = 27.sp, fontWeight = FontWeight.Light, letterSpacing = 7.sp)
            Spacer(Modifier.height(8.dp))
            Text("PERSONAL ASSISTANT SYSTEM", color = Color(0xFF6B9EB8), fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(34.dp))
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().rotate(rotation)) {
                    val stroke = 2.dp.toPx()
                    drawCircle(Cyan.copy(alpha = .14f), style = Stroke(stroke))
                    drawArc(Cyan, 205f, 95f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Blue, 20f, 65f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Cyan.copy(alpha = .65f), 120f, 28f, false, style = Stroke(1.dp.toPx()))
                    for (i in 0 until 48) {
                        val a = Math.toRadians(i * 7.5)
                        val r1 = size.minDimension * .43f
                        val r2 = r1 + if (i % 4 == 0) 10.dp.toPx() else 4.dp.toPx()
                        val cx = size.width / 2; val cy = size.height / 2
                        drawLine(if (i % 4 == 0) Cyan.copy(alpha = .85f) else Blue.copy(alpha = .55f),
                            androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(a).toFloat() * r1, cy + kotlin.math.sin(a).toFloat() * r1),
                            androidx.compose.ui.geometry.Offset(cx + kotlin.math.cos(a).toFloat() * r2, cy + kotlin.math.sin(a).toFloat() * r2),
                            if (i % 4 == 0) 2.dp.toPx() else 1.dp.toPx())
                    }
                }
                Canvas(Modifier.size(210.dp).rotate(-rotation * .7f)) {
                    drawCircle(Blue.copy(alpha = .22f), style = Stroke(1.dp.toPx()))
                    drawArc(Blue.copy(alpha = .9f), 35f, 125f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Cyan.copy(alpha = .75f), 220f, 55f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
                Box(Modifier.size((138 * pulse).dp).background(Color(0xFF06223A), CircleShape), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) { drawCircle(Cyan.copy(alpha = .9f), style = Stroke(2.dp.toPx())); drawCircle(Blue.copy(alpha = .3f)) }
                    Text("J", color = Cyan, fontSize = 68.sp, fontWeight = FontWeight.ExtraLight)
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(status, color = Cyan, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Text(response, color = Color(0xFFD8F5FF), fontSize = 15.sp, maxLines = 2)
            Spacer(Modifier.height(28.dp))
            Button(onClick = onMic, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF073A5A), contentColor = Cyan), modifier = Modifier.size(78.dp), shape = CircleShape) {
                Text("MIC", fontSize = 14.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("TAP TO SPEAK", color = Color(0xFF507D92), fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(26.dp))
            Text("VOICE INTERFACE • PROTOTYPE", color = Color(0xFF34576A), fontSize = 9.sp, letterSpacing = 1.5.sp)
        }
    }
}
