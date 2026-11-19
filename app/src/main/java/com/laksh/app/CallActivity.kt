package com.laksh.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

/** Full-screen incoming call that shows over the lock screen. */
class CallActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val ended = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val id = intent.getStringExtra(CallService.EXTRA_ID)
        val reminder = id?.let { Reminders.get(this, it) }
        if (reminder == null || CallService.activeId != id) {
            finish()
            return
        }

        ContextCompat.registerReceiver(
            this,
            ended,
            IntentFilter(CallService.ACTION_ENDED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            CallScreen(
                reminder = reminder,
                onAnswer = {
                    sendToService(CallService.ACTION_ANSWER, null)
                    playContent(reminder)
                },
                onDecline = { sendToService(CallService.ACTION_END, "declined") },
                onSnooze = { sendToService(CallService.ACTION_END, "snooze") },
                onDone = { sendToService(CallService.ACTION_END, "done") },
            )
        }
    }

    private fun sendToService(action: String, outcome: String?) {
        val i = Intent(this, CallService::class.java).setAction(action)
        if (outcome != null) i.putExtra(CallService.EXTRA_OUTCOME, outcome)
        startService(i)
    }

    private fun callAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun requestAudioFocus() {
        val am = getSystemService(AudioManager::class.java) ?: return
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(callAudioAttributes())
            .build()
        audioFocusRequest = req
        am.requestAudioFocus(req)
    }

    private fun playContent(r: Reminder) {
        try {
            val path = r.audioPath
            if (path != null && File(path).exists()) {
                requestAudioFocus()
                player = MediaPlayer().apply {
                    setAudioAttributes(callAudioAttributes())
                    setDataSource(path)
                    prepare()
                    start()
                }
            } else if (r.speak) {
                requestAudioFocus()
                tts = TextToSpeech(this) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.setAudioAttributes(callAudioAttributes())
                        tts?.speak("${r.caller} says: ${r.task}", TextToSpeech.QUEUE_FLUSH, null, "laksh")
                    }
                }
            }
        } catch (e: Exception) {
            // The task is still shown on screen.
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(ended) }
        runCatching { player?.release() }
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        audioFocusRequest?.let { req ->
            runCatching { getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(req) }
        }
        super.onDestroy()
    }
}

@Composable
private fun CallScreen(
    reminder: Reminder,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
) {
    var answered by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    BackHandler(enabled = true) { }

    LaunchedEffect(answered) {
        if (answered) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0C1118), Color(0xFF16202C)))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (answered) "Laksh reminder" else "Incoming reminder call",
                color = Color(0xFF9FB0C4),
                fontSize = 13.sp,
                letterSpacing = 1.2.sp,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2E3D)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Fmt.initials(reminder.caller),
                        color = Color(0xFF7FE0B4),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    reminder.caller,
                    color = Color(0xFFF2F6FB),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (answered) "%02d:%02d".format(seconds / 60, seconds % 60) else "mobile · ringing…",
                    color = Color(0xFF9FB0C4),
                    fontSize = 16.sp,
                )
                if (answered) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        reminder.task,
                        color = Color(0xFFF2F6FB),
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B2836))
                            .padding(16.dp),
                    )
                }
            }

            if (!answered) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RoundAction("Decline", Color(0xFFD8443A), rotate = 135f, onClick = onDecline)
                    RoundAction("Answer", Color(0xFF1FA463), rotate = 0f) {
                        answered = true
                        onAnswer()
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RoundAction("Snooze 5 min", Color(0xFF2C3A4A), rotate = 0f, label = "+5", onClick = onSnooze)
                    RoundAction("Done", Color(0xFFD8443A), rotate = 135f, onClick = onDone)
                }
            }
        }
    }
}

@Composable
private fun RoundAction(
    text: String,
    color: Color,
    rotate: Float,
    label: String? = null,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (label != null) {
                Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Image(
                    painterResource(R.drawable.ic_call),
                    contentDescription = text,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(rotate),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text, color = Color(0xFFCFDAE7), fontSize = 14.sp)
    }
}
