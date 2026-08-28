package com.vegerot.larklish

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "LarkListener"
private const val LARK = "com.larksuite.suite"
private const val RELAY_ID = 1 // one Relay per Original key (the tag), so the id is constant
private const val ERRORS_CHANNEL = "errors"
private const val ERROR_ID = 2
private const val MAX_TRANSLATE_CHARS = 1000 // the translation API's limit

/**
 * Layer 3: turn every Lark Original into a Relay, and withdraw the Relay when Lark withdraws.
 * Layer 5: then fetch the Full text and Update the Relay with its translation.
 */
class LarkListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val translator = defaultTranslator()
    private val recorder by lazy { Recorder(File(filesDir, "events.jsonl")) }
    private val fetcher by lazy { defaultMessageFetcher(this) }
    private val updates = HashMap<String, Job>() // in-flight Update per Original key
    private lateinit var manager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(RELAY_CHANNEL, "Relays", NotificationManager.IMPORTANCE_HIGH),
        )
        manager.createNotificationChannel(
            NotificationChannel(ERRORS_CHANNEL, "Errors (debug)", NotificationManager.IMPORTANCE_DEFAULT),
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
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        val preview = Preview.parse(text)
        // A translate failure (no model, no network) would crash the service. The model is on the
        // phone after the first Layer 2 run, so this is unlikely; Android restarts the service.
        // Lark reuses one key per chat: a newer Original on the same key makes the older
        // Update wrong, so cancel it (plan.md Layer 5).
        updates.remove(sbn.key)?.cancel()
        updates[sbn.key] = scope.launch {
            val relayTitle = translator.englishOf(title)
            val relay = buildRelay(this@LarkListener, sbn, relayTitle, preview, translator.englishOf(preview.message))
            manager.notify(sbn.key, RELAY_ID, relay)
            val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            recorder.relayed(sbn.key, title, text, relayTitle, relayText)
            // Debug builds keep the Original next to the Relay for comparison while we
            // develop. Release builds cancel it (Max, 2026-08-25).
            if (!BuildConfig.DEBUG) cancelNotification(sbn.key)
            Log.i(TAG, "relayed key=${sbn.key} title=[$title] text=[$relayText]")
            update(sbn, title, relayTitle, preview)
        }
    }

    /** Layer 5: fetch the Full text, translate it, and Update the Relay in place. */
    private suspend fun update(sbn: StatusBarNotification, title: String, relayTitle: String, preview: Preview) {
        try {
            when (val pick = fetcher.fullTextOf(title, preview, sbn.postTime)) {
                is Pick.Skipped -> recorder.skipped(sbn.key, pick.reason)
                is Pick.Found -> {
                    val fullText = pick.candidate.text
                    val relay = buildRelay(this, sbn, relayTitle, preview, translator.englishOf(fullText.take(MAX_TRANSLATE_CHARS)))
                    manager.notify(sbn.key, RELAY_ID, relay)
                    val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                    recorder.updated(sbn.key, pick.candidate.msgType, fullText, relayText)
                    Log.i(TAG, "updated key=${sbn.key} text=[$relayText]")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "update failed key=${sbn.key}: $e")
            recorder.skipped(sbn.key, "error: $e")
            if (BuildConfig.DEBUG) manager.notify(ERROR_ID, errorNotification(e))
        }
    }

    private fun errorNotification(e: Exception): Notification =
        Notification.Builder(this, ERRORS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Larklish Update failed")
            .setContentText(e.toString())
            .setStyle(Notification.BigTextStyle().bigText(e.toString()))
            .build()

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
        reason: Int,
    ) {
        if (sbn.packageName != LARK) return
        // Lark's cancelAll() also removes Android's autogroup summary (Experiment 04). Skip it.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        recorder.removed(sbn.key, reason)
        // Our own cancelNotification() above also lands here; the Relay must survive that.
        if (reason == REASON_LISTENER_CANCEL) return
        manager.cancel(sbn.key, RELAY_ID)
        Log.i(TAG, "original removed key=${sbn.key} reason=$reason; relay canceled")
    }
}
