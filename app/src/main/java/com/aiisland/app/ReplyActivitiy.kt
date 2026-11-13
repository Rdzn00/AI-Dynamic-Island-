package com.aiisland.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class ReplyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.load(this)
        Bus.textActive.value = true
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = C.Mint, onPrimary = Color.Black)) {
                ReplyScreen { finish() }
            }
        }
    }

    override fun onDestroy() {
        Bus.textActive.value = false
        super.onDestroy()
    }
}

@Composable
private fun ReplyScreen(onClose: () -> Unit) {
    val target = remember { Bus.replyTo.value }
    var text by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(250L)
        fr.requestFocus()
    }

    fun send() {
        val t = text.trim()
        if (t.isEmpty() || target == null) return
        val ok = Bus.sendReply(target, t)
        Bus.voiceText.value = if (ok) t else L.t("send_failed")
        Bus.voice.value = if (ok) Voice.SENT else Voice.ERROR
        onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF11151C))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (target != null) L.t("reply_to", target.title) else L.t("reply_text"),
                color = C.Soft,
                fontSize = 13.sp,
                maxLines = 1
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fr),
                placeholder = { Text(L.t("reply_hint")) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                maxLines = 4
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onClose() }) { Text(L.t("cancel")) }
                Button(onClick = { send() }) { Text(L.t("send")) }
            }
        }
    }
}
