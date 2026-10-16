package com.example.jarvis

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        setContent {
            JarvisScreen { message ->
                tts?.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis_message"
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun JarvisScreen(onSpeak: (String) -> Unit) {
    var status by remember {
        mutableStateOf("SYSTEM ONLINE")
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
            onClick = {
                status = "HELLO, BOSS"
                onSpeak("Hello, boss. JARVIS is online and ready.")
            },
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
    }
}