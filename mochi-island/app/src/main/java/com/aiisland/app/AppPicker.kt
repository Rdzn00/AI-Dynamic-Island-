package com.aiisland.app

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap?)

private val bankWords = setOf(
    "maybank", "mae", "cimb", "octo", "rhb", "hlb", "ambank", "affin", "bsn", "mybsn",
    "muamalat", "uob", "ocbc", "hsbc", "agrobank", "mbsb", "tng", "boost", "grabpay",
    "paypal", "wise", "revolut", "binance", "maya", "gcash"
)

private val bankPhrases = listOf(
    "hong leong", "pb engage", "standard chartered", "touch n go", "touch 'n go", "alliance"
)

fun looksLikeBank(label: String, pkg: String): Boolean {
    val l = label.lowercase()
    val p = pkg.lowercase()
    if (l.contains("bank") || p.contains("bank")) return true
    val words = l.split(" ", "-", "'", ".").filter { it.isNotEmpty() }.toSet()
    if (words.any { it in bankWords }) return true
    return bankPhrases.any { l.contains(it) }
}

fun loadApps(c: Context, icons: Boolean): List<AppItem> {
    val pm = c.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val out = ArrayList<AppItem>()
    val seen = HashSet<String>()
    for (ri in pm.queryIntentActivities(intent, 0)) {
        val pkg = ri.activityInfo.packageName
        if (pkg == c.packageName) continue
        if (!seen.add(pkg)) continue
        val label = ri.loadLabel(pm).toString()
        var icon: ImageBitmap? = null
        if (icons) {
            try {
                icon = ri.loadIcon(pm).toBitmap(72, 72).asImageBitmap()
            } catch (e: Exception) {
            }
        }
        out.add(AppItem(label, pkg, icon))
    }
    return out.sortedBy { it.label.lowercase() }
}

@Composable
fun AppPicker(
    title: String,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val first = selected
        val all = withContext(Dispatchers.IO) { loadApps(ctx, true) }
        apps = all.sortedWith(compareByDescending<AppItem> { it.pkg in first }.thenBy { it.label.lowercase() })
        loaded = true
    }
    BackHandler { onClose() }
    val list = remember(apps, q) {
        if (q.isBlank()) apps else apps.filter { it.label.contains(q, true) || it.pkg.contains(q, true) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1220), C.Bg)))
            .systemBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "←",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${selected.size} " + L.t("selected"), color = C.Soft, fontSize = 13.sp)
            }
        }
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            placeholder = { Text(L.t("search")) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        if (!loaded) {
            Text(
                L.t("loading"),
                color = C.Soft,
                fontSize = 14.sp,
                modifier = Modifier.padding(20.dp)
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(list, key = { it.pkg }) { a ->
                val on = a.pkg in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onChange(if (on) selected - a.pkg else selected + a.pkg) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ic = a.icon
                    if (ic != null) {
                        Image(
                            ic,
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                    } else {
                        Spacer(Modifier.size(38.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.pkg, color = C.Soft, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Checkbox(
                        checked = on,
                        onCheckedChange = { onChange(if (on) selected - a.pkg else selected + a.pkg) }
                    )
                }
            }
        }
    }
}
