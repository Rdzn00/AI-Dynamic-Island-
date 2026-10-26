package com.aiisland.app

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {
    private var ctrl: MediaController? = null
    private var manager: MediaSessionManager? = null
    private var lastArt: Bitmap? = null
    private var lastAccent = 0xFF7CF0C8.toInt()

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list)
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val m = getSystemService(MediaSessionManager::class.java)
        manager = m
        val me = ComponentName(this, NotificationService::class.java)
        try {
            m.addOnActiveSessionsChangedListener(sessionsListener, me)
            pick(m.getActiveSessions(me))
        } catch (e: SecurityException) {
        }
        Bus.mediaPrev = { ctrl?.transportControls?.skipToPrevious() }
        Bus.mediaNext = { ctrl?.transportControls?.skipToNext() }
        Bus.mediaToggle = {
            val c = ctrl
            if (c != null) {
                if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    c.transportControls.pause()
                } else {
                    c.transportControls.play()
                }
            }
        }
        Bus.dismissNote = { n -> cancelNotification(n.key) }
        Bus.sendReply = { n, t -> sendReplyTo(n, t) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            manager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (e: Exception) {
        }
        ctrl?.unregisterCallback(callback)
        ctrl = null
        Bus.media.value = null
        Bus.dismissNote = {}
        Bus.sendReply = { _, _ -> false }
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        ctrl?.unregisterCallback(callback)
        ctrl = best
        best?.registerCallback(callback)
        publish()
    }

    private fun label(pkg: String): String {
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun accentOf(bmp: Bitmap?): Int {
        val mint = 0xFF7CF0C8.toInt()
        if (bmp == null) return mint
        if (bmp === lastArt) return lastAccent
        var c = mint
        try {
            val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(px, hsv)
            hsv[1] = maxOf(hsv[1], 0.35f)
            hsv[2] = maxOf(hsv[2], 0.85f)
            c = android.graphics.Color.HSVToColor(hsv)
        } catch (e: Exception) {
        }
        lastArt = bmp
        lastAccent = c
        return c
    }

    private fun publish() {
        val c = ctrl
        if (c == null) {
            Bus.media.value = null
            return
        }
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = (md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty()
        val ps = c.playbackState
        if (ps == null || title.isBlank()) {
            Bus.media.value = null
            return
        }
        val st = ps.state
        if (st == PlaybackState.STATE_NONE || st == PlaybackState.STATE_STOPPED) {
            Bus.media.value = null
            return
        }
        val playing = st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val dur = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        Bus.media.value = Media(
            label(c.packageName),
            title,
            artist,
            playing,
            art,
            accentOf(art),
            ps.position,
            dur,
            ps.lastPositionUpdateTime,
            ps.playbackSpeed
        )
    }

    private fun sendReplyTo(note: Note, text: String): Boolean {
        val a = note.reply ?: return false
        val inputs = a.remoteInputs ?: return false
        return try {
            val intent = Intent()
            val bundle = Bundle()
            for (ri in inputs) {
                bundle.putCharSequence(ri.resultKey, text)
            }
            RemoteInput.addResultsToIntent(inputs, intent, bundle)
            a.actionIntent.send(this, 0, intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val s = sbn ?: return
        val n = s.notification ?: return
        if (s.packageName == packageName) return
        if (Bus.blockOn.value && s.packageName in Bus.blocked.value) return
        if (s.isOngoing) return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if (n.category == Notification.CATEGORY_TRANSPORT) return
        val extras = n.extras ?: return
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        val text = body?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val reply = n.actions?.firstOrNull { a ->
            val ri = a.remoteInputs
            ri != null && ri.isNotEmpty()
        }
        Bus.note.value = Note(System.nanoTime(), s.key, label(s.packageName), title, text, reply)
    }
}
