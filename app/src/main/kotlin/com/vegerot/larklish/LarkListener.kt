package com.vegerot.larklish

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "LarkListener"
private const val LARK = "com.larksuite.suite"
private const val RELAY_ID = 1 // one Relay per Original key (the tag), so the id is constant

/** Layer 3: turn every Lark Original into a Relay, and withdraw the Relay when Lark withdraws. */
class LarkListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val translator: Translator = MlKitTranslator()
    private val recorder by lazy { Recorder(File(filesDir, "events.jsonl")) }
    private lateinit var manager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(RELAY_CHANNEL, "Relays", NotificationManager.IMPORTANCE_HIGH),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.i(TAG, "connected; active Lark notifications: ${activeNotifications.count { it.packageName == LARK }}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != LARK) return
        val n = sbn.notification
        // Android's autogroup summary for Lark: no title, no text (Experiment 01).
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        val preview = Preview.parse(n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        // A translate failure (no model, no network) would crash the service. The model is on the
        // phone after the first Layer 2 run, so this is unlikely; Android restarts the service.
        scope.launch {
            val relay = buildRelay(
                this@LarkListener, sbn,
                translator.englishOf(title), preview, translator.englishOf(preview.message),
            )
            manager.notify(sbn.key, RELAY_ID, relay)
            val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            recorder.relayed(sbn.key, title, preview.run { if (sender.isEmpty()) message else "$sender: $message" },
                relay.extras.getCharSequence(Notification.EXTRA_TITLE).toString(), relayText)
            // Debug builds keep the Original next to the Relay for comparison while we
            // develop. Release builds cancel it (Max, 2026-08-25).
            if (!BuildConfig.DEBUG) cancelNotification(sbn.key)
            Log.i(TAG, "relayed key=${sbn.key} title=[$title] text=[$relayText]")
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
        reason: Int,
    ) {
        if (sbn.packageName != LARK) return
        recorder.removed(sbn.key, reason)
        // Our own cancelNotification() above also lands here; the Relay must survive that.
        if (reason == REASON_LISTENER_CANCEL) return
        manager.cancel(sbn.key, RELAY_ID)
        Log.i(TAG, "original removed key=${sbn.key} reason=$reason; relay canceled")
    }
}
