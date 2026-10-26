package com.aiisland.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val Gold = Color(0xFFE6C27A)
private val Orange = Color(0xFFFF8A5B)

@Composable
private fun PageTitle(title: String, sub: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(sub, color = C.Soft, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun HomeScreen(svcOn: Boolean, onToggle: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    var s by remember { mutableStateOf(Stats()) }
    val hist = remember { mutableStateListOf<Float>() }
    LaunchedEffect(Unit) {
        while (true) {
            val r = withContext(Dispatchers.IO) { readStats(ctx) }
            s = r
            hist.add(if (r.cpu >= 0) r.cpu / 100f else 0f)
            if (hist.size > 30) hist.removeAt(0)
            delay(1500L)
        }
    }
    val islandOn by Bus.islandOn
    val ramPct = if (s.ramTotal > 0f) s.ramUsed / s.ramTotal else 0f
    val t = "${L.t("temp")} ${s.tempC.toInt()}°C"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header(svcOn && islandOn)
        QuickStatus(svcOn, islandOn, onToggle)
        Text(L.t("stats"), color = C.Soft, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f), "battery", C.Mint, L.t("battery"),
                "${s.battery}", "%",
                t + (if (s.charging) " · " + L.t("charging_w") else "")
            ) { Meter(s.battery / 100f, C.Mint) }
            StatCard(
                Modifier.weight(1f), "ram", C.Cyan, L.t("ram"),
                fmt(s.ramUsed), "/ ${fmt(s.ramTotal)} GB",
                L.t("usage") + ": ${(ramPct * 100f).toInt()}%"
            ) { Meter(ramPct, C.Cyan) }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .glass(C.Purple, C.Purple, 22.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(C.Purple.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Ico("wave", C.Purple, 22.dp) }
                Spacer(Modifier.width(10.dp))
                Text(L.t("cpu"), color = C.Soft, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (s.cpu >= 0) {
                    Text("${s.cpu}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Wave(hist, C.Purple)
            Text(
                if (s.cpu >= 0) t + " · " + L.t("estimate") + " ${fmt(s.ghz)} GHz" else L.t("cpu_blocked"),
                color = C.Soft,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(L.t("home_note"), color = C.Soft, fontSize = 12.sp)
    }
}

@Composable
private fun Header(active: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .glass(C.Cyan, C.Mint, 40.dp)
                .padding(horizontal = 40.dp, vertical = 10.dp)
        ) {
            Robot(if (active) Mood.HAPPY else Mood.SLEEPY, 72.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text("Mochi Dynamic Island v0.5.5", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(L.t("home_sub"), color = C.Soft, fontSize = 13.sp)
    }
}

@Composable
private fun QuickStatus(svcOn: Boolean, on: Boolean, onToggle: (Boolean) -> Unit) {
    val active = svcOn && on
    val title = if (!svcOn) L.t("island_disc") else if (on) L.t("island_on") else L.t("island_off")
    val sub = if (!svcOn) L.t("sub_enable") else if (on) L.t("sub_run") else L.t("sub_hidden")
    val color by animateColorAsState(if (active) C.Mint else C.Soft, tween(300), label = "qs")
    Row(
        Modifier
            .fillMaxWidth()
            .glass(C.Purple, C.Cyan, 24.dp)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(L.t("status_label"), color = C.Soft, fontSize = 13.sp)
            Text(title, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = C.Soft, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        MochiSwitch(active, onToggle)
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: String,
    tint: Color,
    title: String,
    value: String,
    unit: String,
    footer: String,
    middle: @Composable () -> Unit
) {
    Column(
        modifier
            .height(176.dp)
            .glass(tint, tint, 22.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Ico(icon, tint, 22.dp) }
            Spacer(Modifier.width(10.dp))
            Text(title, color = C.Soft, fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (unit.isNotEmpty()) {
                Text(" " + unit, color = C.Soft, fontSize = 13.sp, maxLines = 1)
            }
        }
        middle()
        Text(footer, color = C.Soft, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Wave(h: List<Float>, color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val n = h.size
        if (n > 2) {
            val xs = FloatArray(n) { size.width * it / (n - 1) }
            val ys = FloatArray(n) { size.height * (1f - h[it].coerceIn(0f, 1f)) }
            val p = Path()
            p.moveTo(xs[0], ys[0])
            for (i in 1 until n - 1) {
                p.quadraticBezierTo(xs[i], ys[i], (xs[i] + xs[i + 1]) / 2f, (ys[i] + ys[i + 1]) / 2f)
            }
            p.lineTo(xs[n - 1], ys[n - 1])
            drawPath(p, color, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun ChatScreen() {
    val said by Bus.voiceText
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Robot(Mood.RELAXED, 110.dp)
        Spacer(Modifier.height(12.dp))
        Text(L.t("chat_title"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(L.t("chat_soon"), color = C.Soft, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (said.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(C.Mint, C.Cyan)
                    .padding(16.dp)
            ) {
                Text(L.t("chat_last"), color = C.Soft, fontSize = 12.sp)
                Text(said, color = Color.White, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .glass(Color.White, Color.White, 28.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(L.t("chat_hint"), color = C.Soft, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OutlineBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .glass(Color.White, Color.White, 18.dp)
            .clickable { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ToolCard(
    color: Color,
    icon: String,
    title: String,
    desc: String,
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    note: String,
    btn: String,
    onBtn: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(color, color, 24.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ico(icon, color, 26.dp)
            Spacer(Modifier.width(10.dp))
            Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(desc, color = Color(0xFFD7DEE8), fontSize = 14.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .glass(Color.White, Color.White, 18.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (on) L.t("on") else L.t("off"),
                color = if (on) C.Mint else C.Soft,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            MochiSwitch(on, onToggle)
        }
        Text(note, color = C.Soft, fontSize = 13.sp)
        OutlineBtn(btn, onBtn)
    }
}

@Composable
fun ToolsScreen(onBanks: () -> Unit, onBlock: () -> Unit) {
    val ctx = LocalContext.current
    val shield by Bus.shieldOn
    val block by Bus.blockOn
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageTitle(L.t("tools_title"), L.t("tools_sub"))
        ToolCard(
            Gold, "shield", L.t("shield_title"), L.t("shield_desc"),
            shield, { v ->
                Bus.shieldOn.value = v
                Bus.save(ctx)
            },
            L.t("shield_note"), L.t("shield_list"), onBanks
        )
        ToolCard(
            Orange, "block", L.t("excl_title"), L.t("excl_desc"),
            block, { v ->
                Bus.blockOn.value = v
                Bus.save(ctx)
            },
            L.t("excl_note"), L.t("excl_list"), onBlock
        )
    }
}

@Composable
private fun SetupRow(title: String, ok: Boolean, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(if (ok) C.Mint else C.Amber)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(
                if (ok) L.t("active") else L.t("not_yet"),
                color = if (ok) C.Mint else C.Amber,
                fontSize = 12.sp
            )
        }
        if (!ok) {
            Button(onClick = onClick) { Text(label) }
        }
    }
}

private fun langName(code: String): String = when (code) {
    "en" -> "English"
    "zh" -> "中文"
    else -> "Bahasa Melayu"
}

@Composable
private fun LangChip(code: String, current: String, modifier: Modifier, onPick: () -> Unit) {
    val sel = code == current
    Box(
        modifier
            .glass(if (sel) C.Mint else Color.White, if (sel) C.Mint else Color.White, 16.dp)
            .clickable { onPick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            langName(code),
            color = if (sel) C.Mint else Color.White,
            fontSize = 14.sp,
            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun LanguageCard() {
    val ctx = LocalContext.current
    val lang by L.lang
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .glass(C.Mint, C.Cyan, 24.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ico("globe", C.Mint, 26.dp)
            Spacer(Modifier.width(10.dp))
            Text(L.t("lang_title"), color = Gold, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(L.t("lang_desc"), color = Color(0xFFD7DEE8), fontSize = 14.sp)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(Color.White, Color.White, 16.dp)
                    .clickable { open = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(langName(lang), color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("▾", color = C.Soft, fontSize = 18.sp)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for (code in listOf("ms", "en", "zh")) {
                    DropdownMenuItem(
                        text = { Text(langName(code)) },
                        onClick = {
                            L.lang.value = code
                            Bus.save(ctx)
                            open = false
                        }
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (code in listOf("ms", "en", "zh")) {
                LangChip(code, lang, Modifier.weight(1f)) {
                    L.lang.value = code
                    Bus.save(ctx)
                }
            }
        }
    }
}

@Composable
private fun CalRow(label: String, value: String, v: Int, lo: Int, hi: Int, onChange: (Int) -> Unit, done: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(value, color = C.Soft, fontSize = 13.sp)
        }
        Slider(
            value = v.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            onValueChangeFinished = done,
            valueRange = lo.toFloat()..hi.toFloat()
        )
    }
}

@Composable
private fun CalibratorCard() {
    val ctx = LocalContext.current
    val pw by Bus.pillW
    val ph by Bus.pillH
    val ox by Bus.offX
    val oy by Bus.offY
    Column(
        Modifier
            .fillMaxWidth()
            .glass(C.Cyan, C.Mint, 24.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ico("scan", C.Cyan, 26.dp)
            Spacer(Modifier.width(10.dp))
            Text(L.t("calib_title"), color = Gold, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(L.t("calib_desc"), color = Color(0xFFD7DEE8), fontSize = 14.sp)
        CalRow(L.t("width"), "$pw dp  (80 - 200)", pw, 80, 200, { Bus.pillW.value = it }) { Bus.save(ctx) }
        CalRow(L.t("height"), "$ph dp  (30 - 60)", ph, 30, 60, { Bus.pillH.value = it }) { Bus.save(ctx) }
        CalRow(L.t("xoff"), "$ox  (-100 - 100)", ox, -100, 100, { Bus.offX.value = it }) { Bus.save(ctx) }
        CalRow(L.t("yoff"), "$oy  (0 - 60)", oy, 0, 60, { Bus.offY.value = it }) { Bus.save(ctx) }
        Box(
            Modifier
                .fillMaxWidth()
                .glass(Color.White, Color.White, 18.dp)
                .clickable {
                    Bus.pillW.value = 132
                    Bus.pillH.value = 38
                    Bus.offX.value = 0
                    Bus.offY.value = 8
                    Bus.save(ctx)
                }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(L.t("reset"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(L.t("reset_sub"), color = C.Soft, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingScreen(
    svcOn: Boolean,
    notifOn: Boolean,
    micOn: Boolean,
    onAccess: () -> Unit,
    onNotif: () -> Unit,
    onMic: () -> Unit,
    onInfo: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageTitle(L.t("set_title"), L.t("set_sub"))
        LanguageCard()
        CalibratorCard()
        Text(L.t("perms"), color = C.Soft, fontSize = 14.sp)
        SetupRow(L.t("perm_island"), svcOn, L.t("btn_enable"), onAccess)
        SetupRow(L.t("perm_notif"), notifOn, L.t("btn_give"), onNotif)
        SetupRow(L.t("perm_mic"), micOn, L.t("btn_give"), onMic)
        Text(L.t("restricted"), color = C.Soft, fontSize = 12.sp)
        Button(onClick = onInfo) { Text(L.t("open_info")) }
    }
}
