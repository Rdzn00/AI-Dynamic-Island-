package com.aiisland.app

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Display
import android.view.Gravity
import android.view.Surface
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.math.sqrt

private class Owner : LifecycleOwner, SavedStateRegistryOwner {
    private val reg = LifecycleRegistry(this)
    private val ctl = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = reg
    override val savedStateRegistry: SavedStateRegistry get() = ctl.savedStateRegistry

    fun start() {
        ctl.performAttach()
        ctl.performRestore(null)
        reg.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        reg.currentState = Lifecycle.State.DESTROYED
    }
}

class IslandService : AccessibilityService(), SensorEventListener {
    companion object {
        @Volatile
        var running = false
    }

    private val phone = mutableStateOf(Phone())
    private val owner = Owner()
    private val handler = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null
    private var shakeAt = 0L
    private var recognizer: SpeechRecognizer? = null
    private var sensors: SensorManager? = null
    private var audio: AudioManager? = null
    private var imePkgs: Set<String> = emptySet()
    private var dm: DisplayManager? = null
    private var wm: WindowManager? = null
    private var shown = false
    private var connected = false

    private val displayCb = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) readDisplay()
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            applyVisibility()
            handler.postDelayed(this, 700L)
        }
    }

    private val audioCb = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudio(true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudio(false)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> L.t("no_speech")
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> L.t("need_mic")
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> L.t("need_net")
                else -> L.t("voice_err", error.toString())
            }
            fail(msg)
        }

        override fun onResults(results: Bundle?) {
            val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            Bus.voiceText.value = t
            Bus.voice.value = Voice.RESULT
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (t.isNotEmpty()) Bus.voiceText.value = t
        }
    }

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> readDisplay()
                Intent.ACTION_SCREEN_ON -> readDisplay()
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> refreshDnd(true)
                else -> refresh()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui" || pkg in imePkgs) return
        if (Bus.fgPkg.value != pkg) {
            Bus.fgPkg.value = pkg
            applyVisibility()
        }
    }

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        readDisplay()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (connected) return
        connected = true
        Bus.load(this)
        running = true
        val imm = getSystemService(InputMethodManager::class.java)
        imePkgs = imm.inputMethodList.map { it.packageName }.toSet()
        owner.start()
        refresh()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        ContextCompat.registerReceiver(this, rx, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        sensors = getSystemService(SensorManager::class.java)
        val am = getSystemService(AudioManager::class.java)
        audio = am
        am.registerAudioDeviceCallback(audioCb, handler)
        refreshAudio(false)
        refreshDnd(false)
        Bus.startVoice = { startVoice() }
        Bus.stopVoice = { stopVoice() }
        val d = getSystemService(DisplayManager::class.java)
        dm = d
        d.registerDisplayListener(displayCb, handler)
        readDisplay()
        handler.postDelayed(poll, 700L)
    }

    override fun onDestroy() {
        running = false
        stopVoice()
        Bus.startVoice = {}
        Bus.stopVoice = {}
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(rx)
        } catch (e: Exception) {
        }
        dm?.unregisterDisplayListener(displayCb)
        audio?.unregisterAudioDeviceCallback(audioCb)
        detachIsland()
        connected = false
        owner.stop()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val v = event?.values ?: return
        val x = v[0]
        val y = v[1]
        val z = v[2]
        val now = SystemClock.elapsedRealtime()
        if (sqrt(x * x + y * y + z * z) / 9.81f > 2.4f && now - shakeAt > 3000) {
            shakeAt = now
            phone.value = phone.value.copy(dizzy = true)
            handler.postDelayed({ phone.value = phone.value.copy(dizzy = false) }, 2200)
        }
        val p = phone.value
        val tilt = ((-x / 4.5f).coerceIn(-1f, 1f) * 20).roundToInt() / 20f
        val down = if (p.faceDown) z < -6f else z < -8f
        if (tilt != p.tilt || down != p.faceDown) {
            phone.value = p.copy(tilt = tilt, faceDown = down)
        }
    }

    private fun refreshAudio(announce: Boolean) {
        val am = audio ?: return
        var has = false
        for (d in am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            val t = d.type
            if (t == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                t == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                t == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                t == AudioDeviceInfo.TYPE_USB_HEADSET ||
                t == AudioDeviceInfo.TYPE_BLE_HEADSET
            ) {
                has = true
            }
        }
        if (has != phone.value.headphones) {
            phone.value = phone.value.copy(headphones = has)
            if (announce && has) {
                Bus.flash.value = Flash(System.nanoTime(), Mood.HEADPHONES, L.t("headphones"), L.t("connected"))
            }
        }
    }

    private fun refreshDnd(announce: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        val f = nm.currentInterruptionFilter
        val on = f != NotificationManager.INTERRUPTION_FILTER_ALL &&
            f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        if (on != phone.value.dnd) {
            phone.value = phone.value.copy(dnd = on)
            if (announce) {
                Bus.flash.value = Flash(
                    System.nanoTime(),
                    if (on) Mood.SLEEP else Mood.HAPPY,
                    L.t("dnd"),
                    if (on) L.t("turned_on") else L.t("turned_off")
                )
            }
        }
    }

    private fun fail(msg: String) {
        Bus.voiceText.value = msg
        Bus.voice.value = Voice.ERROR
    }

    private fun startVoice() {
        stopVoice()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail(L.t("need_mic"))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            fail(L.t("no_recog"))
            return
        }
        Bus.voiceText.value = ""
        Bus.voice.value = Voice.LISTENING
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        r.setRecognitionListener(listener)
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ms-MY")
        r.startListening(i)
        recognizer = r
    }

    private fun stopVoice() {
        recognizer?.destroy()
        recognizer = null
        Bus.voice.value = Voice.IDLE
    }

    private fun refresh() {
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        phone.value = phone.value.copy(
            battery = if (level >= 0 && scale > 0) level * 100 / scale else 80,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        )
    }

    private fun readDisplay() {
        val d = dm?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val st = d.state
        Bus.screenOn.value = st == Display.STATE_ON || st == Display.STATE_ON_SUSPEND
        val r = d.rotation
        Bus.landscape.value = r == Surface.ROTATION_90 || r == Surface.ROTATION_270
        applyVisibility()
    }

    private fun wantShow(): Boolean {
        val bank = Bus.shieldOn.value && Bus.fgPkg.value in Bus.banks.value
        return Bus.islandOn.value && Bus.screenOn.value && !Bus.landscape.value && !bank
    }

    private fun applyVisibility() {
        if (!connected) return
        val want = wantShow()
        if (want == shown) return
        if (want) attachIsland() else detachIsland()
    }

    private fun detachIsland() {
        sensors?.unregisterListener(this)
        val v = view
        if (v != null) {
            try {
                wm?.removeView(v)
            } catch (e: Exception) {
            }
        }
        view = null
        shown = false
        Bus.stopVoice()
        Bus.note.value = null
        Bus.replyTo.value = null
        Bus.flash.value = null
    }

    private fun attachIsland() {
        val w = getSystemService(WindowManager::class.java)
        wm = w
        val density = resources.displayMetrics.density
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.x = (Bus.offX.value * density).toInt()
        lp.y = (Bus.offY.value * density).toInt()
        if (Build.VERSION.SDK_INT >= 30) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        Bus.note.value = null
        Bus.replyTo.value = null
        Bus.flash.value = null
        refresh()

        val v = ComposeView(this)
        v.setViewTreeLifecycleOwner(owner)
        v.setViewTreeSavedStateRegistryOwner(owner)
        v.setContent {
            val ox by Bus.offX
            val oy by Bus.offY
            LaunchedEffect(ox, oy) {
                lp.x = (ox * density).toInt()
                lp.y = (oy * density).toInt()
                try {
                    w.updateViewLayout(v, lp)
                } catch (e: Exception) {
                }
            }
            Island(phone.value)
        }
        try {
            w.addView(v, lp)
        } catch (e: Exception) {
            return
        }
        view = v
        shown = true
        val sm = sensors
        if (sm != null) {
            sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
        }
    }
}
