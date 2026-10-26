package com.aiisland.app

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Mode { SMALL, INFO, NOTE, VOICE, MUSIC, CHARGE, FLASH }

private val Soft = Color(0xFF9AA3B2)
private val Mint = Color(0xFF7CF0C8)

@Composable
fun Island(phone: Phone) {
    var open by remember { mutableStateOf(false) }
    var activity by remember { mutableStateOf(0) }
    var micro by remember { mutableStateOf<Mood?>(null) }
    var chargeShow by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val note by Bus.note
    val replyTo by Bus.replyTo
    val flash by Bus.flash
    val voice by Bus.voice
    val voiceText by Bus.voiceText
    val media by Bus.media
    val pw by Bus.pillW
    val ph by Bus.pillH

    fun close() {
        open = false
        chargeShow = false
        Bus.note.value = null
        Bus.replyTo.value = null
        Bus.flash.value = null
        Bus.stopVoice()
        Bus.voice.value = Voice.IDLE
    }

    LaunchedEffect(note?.id) {
        if (note != null) {
            open = true
            activity += 1
        }
    }
    LaunchedEffect(voice) {
        if (voice != Voice.IDLE) {
            activity += 1
        }
        if (voice == Voice.SENT) {
            delay(1600L)
            close()
        }
    }
    LaunchedEffect(media?.title) {
        val md = media
        if (md != null && md.playing && md.title.isNotEmpty()) {
            open = true
            activity += 1
        }
    }
    LaunchedEffect(flash?.id) {
        val f = flash
        if (f != null) {
            open = true
            activity += 1
            delay(3000L)
            if (Bus.flash.value?.id == f.id) {
                Bus.flash.value = null
                if (Bus.note.value == null && Bus.voice.value == Voice.IDLE) close()
            }
        }
    }
    val chargeKey = if (!phone.charging) 0 else if (phone.battery >= 100) 2 else 1
    LaunchedEffect(chargeKey) {
        if (chargeKey == 0) {
            chargeShow = false
        } else if (seen) {
            chargeShow = true
            open = true
            activity += 1
            delay(4500L)
            chargeShow = false
            if (Bus.note.value == null && Bus.voice.value == Voice.IDLE && Bus.flash.value == null) close()
        }
        seen = true
    }
    LaunchedEffect(open, activity) {
        if (open) {
            delay(10_000L)
            close()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(4000L, 9000L))
            micro = listOf(Mood.HAPPY, Mood.SURPRISED, Mood.RELAXED).random()
            delay(1800L)
            micro = null
        }
    }

    val base = moodOf(phone)
    val m = micro
    val fl = flash
    val cur = note
    val md = media
    val dancing = md?.playing == true && !phone.dizzy && !phone.faceDown
    val mood = when {
        voice == Voice.LISTENING -> Mood.LISTENING
        voice == Voice.RESULT || voice == Voice.SENT -> Mood.HAPPY
        voice == Voice.ERROR -> Mood.SURPRISED
        cur != null && open -> Mood.HAPPY
        fl != null && open -> fl.mood
        chargeShow && open -> if (phone.battery >= 100) Mood.PARTY else Mood.ENERGY
        dancing -> Mood.DANCE
        m != null && base == Mood.NEUTRAL -> m
        else -> base
    }
    val mode = when {
        !open -> Mode.SMALL
        voice != Voice.IDLE -> Mode.VOICE
        cur != null -> Mode.NOTE
        fl != null -> Mode.FLASH
        chargeShow -> Mode.CHARGE
        md != null -> Mode.MUSIC
        else -> Mode.INFO
    }
    val confirming = voice == Voice.RESULT && replyTo != null

    val tw = when (mode) {
        Mode.SMALL -> pw.dp
        Mode.INFO -> 300.dp
        Mode.NOTE -> 340.dp
        Mode.VOICE -> 340.dp
        Mode.MUSIC -> 340.dp
        Mode.CHARGE -> 300.dp
        Mode.FLASH -> 300.dp
    }
    val th = when (mode) {
        Mode.SMALL -> ph.dp
        Mode.INFO -> 76.dp
        Mode.NOTE -> 104.dp
        Mode.VOICE -> if (confirming) 140.dp else 96.dp
        Mode.MUSIC -> 176.dp
        Mode.CHARGE -> 88.dp
        Mode.FLASH -> 76.dp
    }
    val tr = if (mode == Mode.SMALL) (ph / 2f).dp else 30.dp
    val anim = spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    val w by animateDpAsState(tw, anim, label = "w")
    val h by animateDpAsState(th, anim, label = "h")
    val r by animateDpAsState(tr, anim, label = "r")

    var boxMod = Modifier
        .size(w, h)
        .graphicsLayer {
            translationX = dragX.value
            translationY = dragY.value
            alpha = 1f - (abs(dragX.value) / 700f + abs(dragY.value) / 400f).coerceIn(0f, 0.7f)
        }
        .clip(RoundedCornerShape(r))
        .background(Color.Black)
    if (mode == Mode.MUSIC && md != null) {
        boxMod = boxMod.background(
            Brush.horizontalGradient(listOf(Color(md.accent).copy(alpha = 0.35f), Color.Black))
        )
    }

    Box(
        boxMod
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (open) {
                            close()
                        } else {
                            open = true
                            activity += 1
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        open = true
                        activity += 1
                        Bus.note.value = null
                        Bus.replyTo.value = null
                        Bus.startVoice()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val far = abs(dragX.value) > 140f || dragY.value < -90f
                            if (far && open) {
                                val n = Bus.note.value
                                if (n != null) Bus.dismissNote(n)
                                close()
                                dragX.snapTo(0f)
                                dragY.snapTo(0f)
                            } else {
                                dragX.animateTo(0f, spring())
                                dragY.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragX.snapTo(0f)
                            dragY.snapTo(0f)
                        }
                    }
                ) { change, amount ->
                    change.consume()
                    if (open) {
                        scope.launch {
                            dragX.snapTo(dragX.value + amount.x)
                            dragY.snapTo((dragY.value + amount.y).coerceAtMost(0f))
                        }
                    }
                }
            }
    ) {
        if (mode == Mode.MUSIC && md != null) {
            MusicLayout(md, mood, phone.tilt) { activity += 1 }
        } else if (mode == Mode.CHARGE) {
            ChargeLayout(phone.battery, mood)
        } else {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .padding(start = if (mode == Mode.SMALL) 8.dp else 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Robot(mood, if (mode == Mode.SMALL) (ph * 0.74f).dp else 48.dp, tilt = phone.tilt)
                if (mode != Mode.SMALL) {
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (mode == Mode.NOTE && cur != null) {
                            NoteInfo(cur)
                        } else if (mode == Mode.VOICE) {
                            VoiceInfo(
                                voice,
                                voiceText,
                                replyTo,
                                {
                                    val target = Bus.replyTo.value
                                    val ok = target != null && Bus.sendReply(target, voiceText)
                                    Bus.voiceText.value = if (ok) L.t("sent") else L.t("send_failed")
                                    Bus.voice.value = if (ok) Voice.SENT else Voice.ERROR
                                    activity += 1
                                },
                                { close() }
                            )
                        } else if (mode == Mode.FLASH && fl != null) {
                            FlashInfo(fl)
                        } else {
                            InfoText(phone, mood)
                        }
                    }
                    if (mode == Mode.NOTE && cur != null && cur.reply != null) {
                        Spacer(Modifier.width(10.dp))
                        PillBtn(L.t("reply"), true) {
                            Bus.replyTo.value = cur
                            Bus.startVoice()
                            activity += 1
                        }
                    }
                }
            }
        }
        if (mode == Mode.SMALL && md != null && md.playing) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
            ) {
                Equalizer(true, 14.dp, Color(md.accent))
            }
        } else if (mode == Mode.SMALL && phone.charging) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
            ) {
                ChargeRing(phone.battery, 20.dp)
            }
        } else if (mode == Mode.SMALL && mood == Mood.SLEEP) {
            val za by rememberInfiniteTransition(label = "z").animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "za"
            )
            Text(
                "zZz",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                color = Soft.copy(alpha = za),
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun mmss(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    return String.format(Locale.US, "%d:%02d", s / 60L, s % 60L)
}

@Composable
private fun MusicLayout(md: Media, mood: Mood, tilt: Float, onActivity: () -> Unit) {
    val accent = Color(md.accent)
    val art = remember(md.art) { md.art?.asImageBitmap() }
    var now by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(md.playing) {
        while (md.playing) {
            now = SystemClock.elapsedRealtime()
            delay(500L)
        }
    }
    val pos = if (md.playing) md.position + ((now - md.posTime) * md.speed).toLong() else md.position
    val frac = if (md.duration > 0L) (pos.toFloat() / md.duration.toFloat()).coerceIn(0f, 1f) else 0f
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (art != null) {
                Image(
                    art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Robot(mood, 48.dp, tilt = tilt)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(md.app, color = Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(md.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(md.artist, color = Color(0xFFC9D1DB), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Equalizer(md.playing, 22.dp, accent)
        }
        if (md.duration > 0L) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mmss(pos), color = Soft, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(mmss(md.duration), color = Soft, fontSize = 11.sp)
            }
        }
        Box(Modifier.fillMaxWidth()) {
            if (art != null) {
                Robot(mood, 36.dp, Modifier.align(Alignment.CenterStart), tilt)
            }
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaBtn("prev", false) {
                    Bus.mediaPrev()
                    onActivity()
                }
                MediaBtn(if (md.playing) "pause" else "play", true) {
                    Bus.mediaToggle()
                    onActivity()
                }
                MediaBtn("next", false) {
                    Bus.mediaNext()
                    onActivity()
                }
            }
        }
    }
}

@Composable
private fun MediaBtn(kind: String, big: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(if (big) 50.dp else 44.dp)
            .clip(CircleShape)
            .background(if (big) Color.White else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Ico(kind, if (big) Color.Black else Color.White, if (big) 26.dp else 24.dp)
    }
}

@Composable
private fun PillBtn(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) Mint else Color(0xFF2A303B))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (primary) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun Equalizer(playing: Boolean, hh: Dp, color: Color) {
    val t = rememberInfiniteTransition(label = "eq")
    Row(
        Modifier.height(hh),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 4) {
            val f by t.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(300 + i * 90), RepeatMode.Reverse),
                label = "eq$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height(hh * (if (playing) f else 0.25f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ChargeLayout(battery: Int, mood: Mood) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Robot(mood, 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (battery >= 100) L.t("full") else L.t("charging"), color = Soft, fontSize = 13.sp)
            Text("$battery%", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        }
        ChargeRing(battery, 58.dp)
    }
}

@Composable
private fun ChargeRing(pct: Int, d: Dp) {
    val sweep by animateFloatAsState(pct.coerceIn(0, 100) * 3.6f, tween(800), label = "ring")
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "glowValue"
    )
    Box(Modifier.size(d), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = this.size.minDimension * 0.12f
            val inset = sw / 2f
            val box = Size(this.size.width - sw, this.size.height - sw)
            drawArc(
                Color.White.copy(alpha = 0.12f),
                0f,
                360f,
                false,
                Offset(inset, inset),
                box,
                style = Stroke(sw)
            )
            drawArc(
                Mint,
                -90f,
                sweep,
                false,
                Offset(inset, inset),
                box,
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Ico("bolt", Color(0xFFFFB454).copy(alpha = glow), d * 0.42f)
    }
}

@Composable
private fun FlashInfo(f: Flash) {
    Column {
        Text(f.sub, color = Soft, fontSize = 12.sp, maxLines = 1)
        Text(f.title, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoText(phone: Phone, mood: Mood) {
    Column {
        Text(
            "${phone.battery}%" + if (phone.charging) " · " + L.t("charge_suffix") else "",
            color = Soft,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )
        Text(L.mood(mood), color = Color.White, fontSize = 16.sp, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun NoteInfo(n: Note) {
    Column {
        Text(n.app, color = Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(n.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(n.text, color = Color(0xFFC9D1DB), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun VoiceInfo(voice: Voice, text: String, reply: Note?, onSend: () -> Unit, onCancel: () -> Unit) {
    val head = if (voice == Voice.LISTENING) {
        if (reply != null) L.t("reply_to", reply.title) else L.t("listening")
    } else if (voice == Voice.RESULT) {
        if (reply != null) L.t("send_to", reply.title) else L.t("said")
    } else if (voice == Voice.SENT) {
        L.t("sent")
    } else {
        L.t("failed")
    }
    Column {
        Text(head, color = Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (voice == Voice.LISTENING && text.isEmpty()) {
            Bars()
        } else {
            Text(text, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (voice == Voice.RESULT && reply != null && text.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillBtn(L.t("cancel"), false) { onCancel() }
                PillBtn(L.t("send"), true) { onSend() }
            }
        }
    }
}

@Composable
private fun Bars() {
    val t = rememberInfiniteTransition(label = "bars")
    Row(
        Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 9) {
            val f by t.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(360 + i * 55), RepeatMode.Reverse),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height((22 * f).dp)
                    .background(Mint, RoundedCornerShape(2.dp))
            )
        }
    }
}
