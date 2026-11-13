package com.aiisland.app

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.mutableStateOf

data class Note(
    val id: Long,
    val key: String,
    val app: String,
    val title: String,
    val text: String,
    val reply: Notification.Action?,
    val content: PendingIntent?
)

data class CallInfo(
    val key: String,
    val name: String,
    val answer: PendingIntent?,
    val decline: PendingIntent?,
    val open: PendingIntent?
)

data class Media(
    val app: String,
    val title: String,
    val artist: String,
    val playing: Boolean,
    val art: Bitmap?,
    val accent: Int,
    val position: Long,
    val duration: Long,
    val posTime: Long,
    val speed: Float
)

data class Flash(val id: Long, val mood: Mood, val title: String, val sub: String)

enum class Voice { IDLE, LISTENING, RESULT, SENT, ERROR }

object Bus {
    val note = mutableStateOf<Note?>(null)
    val replyTo = mutableStateOf<Note?>(null)
    val flash = mutableStateOf<Flash?>(null)
    val call = mutableStateOf<CallInfo?>(null)
    val faceId = mutableStateOf(0L)
    val textActive = mutableStateOf(false)
    val voice = mutableStateOf(Voice.IDLE)
    val voiceText = mutableStateOf("")
    var startVoice: () -> Unit = {}
    var stopVoice: () -> Unit = {}
    var endVoice: () -> Unit = {}
    var dismissNote: (Note) -> Unit = {}
    var sendReply: (Note, String) -> Boolean = { _, _ -> false }

    val media = mutableStateOf<Media?>(null)
    var mediaPrev: () -> Unit = {}
    var mediaToggle: () -> Unit = {}
    var mediaNext: () -> Unit = {}

    val screenOn = mutableStateOf(true)
    val landscape = mutableStateOf(false)

    val blockOn = mutableStateOf(true)
    val blocked = mutableStateOf<Set<String>>(emptySet())
    val pillW = mutableStateOf(132)
    val pillH = mutableStateOf(38)

    val islandOn = mutableStateOf(true)
    val offX = mutableStateOf(0)
    val offY = mutableStateOf(8)

    fun load(c: Context) {
        val p = c.getSharedPreferences("mochi", Context.MODE_PRIVATE)
        islandOn.value = p.getBoolean("on", true)
        offX.value = p.getInt("x", 0)
        offY.value = p.getInt("y", 8)
        pillW.value = p.getInt("pw", 132)
        pillH.value = p.getInt("ph", 38)
        blockOn.value = p.getBoolean("block", true)
        blocked.value = (p.getStringSet("blocked", emptySet()) ?: emptySet()).toSet()
        L.lang.value = p.getString("lang", "ms") ?: "ms"
    }

    fun save(c: Context) {
        c.getSharedPreferences("mochi", Context.MODE_PRIVATE).edit()
            .putBoolean("on", islandOn.value)
            .putInt("x", offX.value)
            .putInt("y", offY.value)
            .putInt("pw", pillW.value)
            .putInt("ph", pillH.value)
            .putBoolean("block", blockOn.value)
            .putStringSet("blocked", blocked.value)
            .putString("lang", L.lang.value)
            .apply()
    }
}

fun firePending(ctx: Context, pi: PendingIntent?) {
    if (pi == null) return
    try {
        if (Build.VERSION.SDK_INT >= 34) {
            val o = ActivityOptions.makeBasic()
            o.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            pi.send(ctx, 0, null, null, null, null, o.toBundle())
        } else {
            pi.send()
        }
    } catch (e: Exception) {
    }
}
