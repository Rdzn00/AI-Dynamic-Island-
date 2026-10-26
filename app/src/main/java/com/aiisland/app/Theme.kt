package com.aiisland.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

object C {
    val Bg = Color(0xFF05070B)
    val Mint = Color(0xFF3DF5A8)
    val Cyan = Color(0xFF22D3EE)
    val Purple = Color(0xFFA78BFA)
    val Amber = Color(0xFFFFB454)
    val Soft = Color(0xFF9AA3B2)
}

fun Modifier.glass(c1: Color, c2: Color = c1, r: Dp = 22.dp): Modifier {
    val shape = RoundedCornerShape(r)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = 0.04f))
        .border(1.dp, Brush.linearGradient(listOf(c1.copy(alpha = 0.7f), c2.copy(alpha = 0.2f))), shape)
}

@Composable
fun MochiSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val pos by animateDpAsState(
        if (checked) 40.dp else 4.dp,
        spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "pos"
    )
    val track by animateColorAsState(
        if (checked) C.Mint.copy(alpha = 0.35f) else Color(0xFF1B212B),
        tween(250),
        label = "track"
    )
    val edge by animateColorAsState(
        if (checked) C.Mint else Color(0xFF3A4350),
        tween(250),
        label = "edge"
    )
    Box(
        Modifier
            .size(76.dp, 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(track)
            .border(1.5.dp, edge, RoundedCornerShape(20.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
    ) {
        Box(
            Modifier
                .offset(x = pos, y = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun Meter(fraction: Float, color: Color) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "meter")
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

@Composable
fun Ico(kind: String, color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val st = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = 2f * s, cap = StrokeCap.Round)
        }

        fun poly(closed: Boolean, vararg v: Float) {
            val p = Path()
            p.moveTo(v[0] * s, v[1] * s)
            var i = 2
            while (i < v.size) {
                p.lineTo(v[i] * s, v[i + 1] * s)
                i += 2
            }
            if (closed) p.close()
            drawPath(p, color, style = st)
        }

        fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            val p = Path()
            p.moveTo(x1 * s, y1 * s)
            p.lineTo(x2 * s, y2 * s)
            p.lineTo(x3 * s, y3 * s)
            p.close()
            drawPath(p, color)
        }

        fun bar(x: Float, y: Float, w: Float, h: Float) {
            drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(1.2f * s))
        }

        fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) {
            drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(r * s), style = st)
        }

        when (kind) {
            "home" -> {
                poly(false, 3f, 11f, 12f, 3f, 21f, 11f)
                poly(false, 5f, 10f, 5f, 21f, 19f, 21f, 19f, 10f)
                poly(false, 10f, 21f, 10f, 15f, 14f, 15f, 14f, 21f)
            }
            "chat" -> {
                rect(4f, 4f, 16f, 12f, 4f)
                poly(false, 8f, 16f, 8f, 21f, 13f, 16f)
                drawCircle(color, 1.1f * s, Offset(9f * s, 10f * s))
                drawCircle(color, 1.1f * s, Offset(12f * s, 10f * s))
                drawCircle(color, 1.1f * s, Offset(15f * s, 10f * s))
            }
            "tools" -> {
                poly(true, 12f, 2.5f, 20f, 7f, 20f, 17f, 12f, 21.5f, 4f, 17f, 4f, 7f)
                poly(false, 4f, 7f, 12f, 12f, 20f, 7f)
                line(12f, 12f, 12f, 21.5f)
            }
            "gear" -> {
                drawCircle(color, 5.5f * s, Offset(12f * s, 12f * s), style = st)
                drawCircle(color, 2f * s, Offset(12f * s, 12f * s), style = st)
                for (k in 0 until 8) {
                    val a = k * 0.7853982f
                    line(12f + 8f * cos(a), 12f + 8f * sin(a), 12f + 10.5f * cos(a), 12f + 10.5f * sin(a))
                }
            }
            "battery" -> {
                rect(2f, 7f, 18f, 10f, 2.5f)
                line(22f, 10.5f, 22f, 13.5f)
            }
            "ram" -> {
                rect(5f, 5f, 14f, 14f, 2f)
                rect(9f, 9f, 6f, 6f, 1f)
                for (v in floatArrayOf(9f, 12f, 15f)) {
                    line(v, 2f, v, 5f)
                    line(v, 19f, v, 22f)
                    line(2f, v, 5f, v)
                    line(19f, v, 22f, v)
                }
            }
            "wave" -> {
                line(4f, 9f, 4f, 15f)
                line(8f, 5f, 8f, 19f)
                line(12f, 2f, 12f, 22f)
                line(16f, 7f, 16f, 17f)
                line(20f, 10f, 20f, 14f)
            }
            "gpu" -> {
                rect(2f, 6f, 20f, 12f, 2f)
                drawCircle(color, 3.5f * s, Offset(10f * s, 12f * s), style = st)
                line(16f, 10f, 19f, 10f)
                line(16f, 14f, 19f, 14f)
            }
            "shield" -> {
                val p = Path()
                p.moveTo(12f * s, 2.5f * s)
                p.lineTo(20f * s, 5.5f * s)
                p.lineTo(20f * s, 11f * s)
                p.cubicTo(20f * s, 16f * s, 16.5f * s, 19.5f * s, 12f * s, 21.5f * s)
                p.cubicTo(7.5f * s, 19.5f * s, 4f * s, 16f * s, 4f * s, 11f * s)
                p.lineTo(4f * s, 5.5f * s)
                p.close()
                drawPath(p, color, style = st)
                poly(false, 8.5f, 12f, 11f, 14.5f, 15.5f, 9.5f)
            }
            "block" -> {
                drawCircle(color, 9f * s, Offset(12f * s, 12f * s), style = st)
                line(5.7f, 5.7f, 18.3f, 18.3f)
            }
            "globe" -> {
                drawCircle(color, 9f * s, Offset(12f * s, 12f * s), style = st)
                line(3f, 12f, 21f, 12f)
                drawOval(color, Offset(8f * s, 3f * s), Size(8f * s, 18f * s), style = st)
            }
            "scan" -> {
                poly(false, 3f, 8f, 3f, 3f, 8f, 3f)
                poly(false, 16f, 3f, 21f, 3f, 21f, 8f)
                poly(false, 21f, 16f, 21f, 21f, 16f, 21f)
                poly(false, 8f, 21f, 3f, 21f, 3f, 16f)
                drawCircle(color, 3f * s, Offset(12f * s, 12f * s), style = st)
            }
            "bolt" -> {
                val p = Path()
                p.moveTo(13f * s, 2f * s)
                p.lineTo(5f * s, 13.5f * s)
                p.lineTo(11f * s, 13.5f * s)
                p.lineTo(10f * s, 22f * s)
                p.lineTo(19f * s, 10f * s)
                p.lineTo(13f * s, 10f * s)
                p.close()
                drawPath(p, color)
            }
            "play" -> tri(7f, 4f, 20f, 12f, 7f, 20f)
            "pause" -> {
                bar(6f, 4f, 4f, 16f)
                bar(14f, 4f, 4f, 16f)
            }
            "next" -> {
                tri(5f, 5f, 15f, 12f, 5f, 19f)
                bar(17f, 5f, 3f, 14f)
            }
            "prev" -> {
                tri(19f, 5f, 9f, 12f, 19f, 19f)
                bar(4f, 5f, 3f, 14f)
            }
            else -> {}
        }
    }
}

@Composable
fun TabBar(tab: Int, onTab: (Int) -> Unit) {
    val items = listOf(
        "home" to L.t("tab_menu"),
        "chat" to L.t("tab_chat"),
        "tools" to L.t("tab_tools"),
        "gear" to L.t("tab_settings")
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .glass(Color.White, Color.White, 28.dp)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEachIndexed { i, item ->
            val on = i == tab
            val col by animateColorAsState(if (on) C.Cyan else C.Soft, tween(250), label = "tabColor")
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onTab(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Ico(item.first, col, 26.dp)
                Text(item.second, color = col, fontSize = 11.sp, maxLines = 1)
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (on) C.Cyan else Color.Transparent)
                )
            }
        }
    }
}
