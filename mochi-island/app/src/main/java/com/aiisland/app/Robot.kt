package com.aiisland.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Mint = Color(0xFF7CF0C8)
private val Amber = Color(0xFFFFB454)
private val Gray = Color(0xFF9AA3B2)
private val Cyan = Color(0xFF22D3EE)
private val HeadColor = Color(0xFFE6EBF0)
private val ScreenColor = Color(0xFF0B0F14)

@Composable
fun Robot(mood: Mood, boxSize: Dp, modifier: Modifier = Modifier, tilt: Float = 0f) {
    val bobMs = when (mood) {
        Mood.SLEEP -> 4200
        Mood.DANCE -> 380
        Mood.ENERGY -> 450
        Mood.PARTY -> 320
        Mood.SLEEPY -> 3200
        else -> 1800
    }
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(bobMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bobValue"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseValue"
    )
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing)),
        label = "spinValue"
    )
    val wob by rememberInfiniteTransition(label = "wob").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse),
        label = "wobValue"
    )
    val tiltAnim by animateFloatAsState(tilt, tween(180), label = "tilt")
    val blink = remember { Animatable(1f) }
    val gaze = remember { Animatable(0f) }

    LaunchedEffect(mood) {
        while (true) {
            delay(Random.nextLong(2500L, 5000L))
            if (mood == Mood.NEUTRAL || mood == Mood.HAPPY) {
                blink.animateTo(0.1f, tween(70))
                blink.animateTo(1f, tween(110))
            }
            if (Random.nextInt(3) == 0) {
                gaze.animateTo(if (Random.nextBoolean()) -1f else 1f, tween(250))
                delay(700)
                gaze.animateTo(0f, tween(300))
            }
        }
    }

    Canvas(modifier.size(boxSize)) {
        val s = this.size.minDimension / 64f

        fun rr(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float) {
            drawRoundRect(c, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(r * s))
        }

        val angle = when (mood) {
            Mood.DIZZY -> wob
            Mood.DANCE -> wob * 1.4f
            Mood.PARTY -> wob * 1.5f
            Mood.ENERGY -> wob * 0.5f
            else -> 0f
        }
        rotate(angle, Offset(32f * s, 40f * s)) {
            translate(top = -2.5f * s * bob) {
                val antenna = when (mood) {
                    Mood.SLEEPY -> Amber
                    Mood.DIZZY -> Amber
                    Mood.SURPRISED -> Amber
                    Mood.ENERGY -> Amber
                    Mood.PARTY -> Amber
                    Mood.SLEEP -> Gray
                    else -> Mint
                }
                drawLine(
                    Gray,
                    Offset(32f * s, 14f * s),
                    Offset(32f * s, 7f * s),
                    strokeWidth = 2.5f * s,
                    cap = StrokeCap.Round
                )
                drawCircle(antenna.copy(alpha = pulse), 3.5f * s, Offset(32f * s, 5f * s))
                rr(Gray, 2f, 26f, 6f, 14f, 3f)
                rr(Gray, 56f, 26f, 6f, 14f, 3f)
                rr(HeadColor, 7f, 14f, 50f, 42f, 16f)
                rr(ScreenColor, 13f, 21f, 38f, 28f, 11f)

                val still = mood == Mood.SLEEP || mood == Mood.DIZZY
                val gx = if (still) 0f else (gaze.value + tiltAnim).coerceIn(-1f, 1f) * 3.5f

                val smile = mood == Mood.HAPPY || mood == Mood.DANCE || mood == Mood.ENERGY ||
                    mood == Mood.PARTY || mood == Mood.HEADPHONES
                if (smile) {
                    for (x in floatArrayOf(20f, 35f)) {
                        val p = Path().apply {
                            moveTo((x + gx) * s, 39f * s)
                            quadraticBezierTo((x + 4.5f + gx) * s, 31f * s, (x + 9f + gx) * s, 39f * s)
                        }
                        drawPath(p, Mint, style = Stroke(width = 3.5f * s, cap = StrokeCap.Round))
                    }
                } else if (mood == Mood.SLEEP) {
                    for (x in floatArrayOf(21f, 36f)) {
                        val p = Path().apply {
                            moveTo(x * s, 36f * s)
                            quadraticBezierTo((x + 3.5f) * s, 41f * s, (x + 7f) * s, 36f * s)
                        }
                        drawPath(p, Mint, style = Stroke(width = 3f * s, cap = StrokeCap.Round))
                    }
                } else if (mood == Mood.DIZZY) {
                    for (x in floatArrayOf(21f, 36f)) {
                        rotate(spin, Offset((x + 3.5f) * s, 35f * s)) {
                            rr(Mint, x, 29f, 7f, 12f, 3.5f)
                        }
                    }
                } else if (mood == Mood.SURPRISED || mood == Mood.LISTENING) {
                    for (x in floatArrayOf(20.5f, 35.5f)) {
                        rr(Mint, x + gx, 27f, 8f, 14f, 4f)
                    }
                    if (mood == Mood.SURPRISED) {
                        drawCircle(Mint, 2.4f * s, Offset(32f * s, 46f * s), style = Stroke(width = 1.8f * s))
                    }
                } else if (mood == Mood.RELAXED) {
                    for (x in floatArrayOf(21f, 36f)) {
                        rr(Mint, x + gx, 33f, 7f, 5f, 2.5f)
                    }
                    val p = Path().apply {
                        moveTo(28.5f * s, 43.5f * s)
                        quadraticBezierTo(32f * s, 47f * s, 35.5f * s, 43.5f * s)
                    }
                    drawPath(p, Mint, style = Stroke(width = 2.2f * s, cap = StrokeCap.Round))
                } else {
                    val open = if (mood == Mood.SLEEPY) 0.4f else blink.value
                    val eyeH = 12f * open
                    for (x in floatArrayOf(21f, 36f)) {
                        rr(Mint, x + gx, 35f - eyeH / 2f, 7f, eyeH, minOf(3.5f, eyeH / 2f))
                    }
                }

                if (mood == Mood.ENERGY) {
                    val b = Path().apply {
                        moveTo(44f * s, 0f)
                        lineTo(39f * s, 8f * s)
                        lineTo(43f * s, 8f * s)
                        lineTo(41f * s, 14f * s)
                        lineTo(49f * s, 5f * s)
                        lineTo(45f * s, 5f * s)
                        close()
                    }
                    drawPath(b, Amber)
                } else if (mood == Mood.PARTY) {
                    for (k in 0 until 6) {
                        val a = (spin + k * 60f) * 0.0174533f
                        drawCircle(
                            if (k % 2 == 0) Mint else Amber,
                            1.8f * s,
                            Offset((32f + 27f * cos(a)) * s, (30f + 24f * sin(a)) * s)
                        )
                    }
                } else if (mood == Mood.HEADPHONES) {
                    val band = Path().apply {
                        moveTo(6f * s, 30f * s)
                        quadraticBezierTo(32f * s, -6f * s, 58f * s, 30f * s)
                    }
                    drawPath(band, Cyan, style = Stroke(width = 3f * s, cap = StrokeCap.Round))
                    rr(Cyan, 0.5f, 25f, 8f, 18f, 4f)
                    rr(Cyan, 55.5f, 25f, 8f, 18f, 4f)
                }
            }
        }
    }
}
