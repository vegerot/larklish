package com.vegerot.larklish

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LarkListener"
private const val LARK = "com.larksuite.suite"
private const val RELAY_ID = 1 // one Relay per Original key (the tag), so the id is constant
private const val ERRORS_CHANNEL = "errors"
private const val ERROR_ID = 2
private const val MAX_TRANSLATE_CHARS = 1000 // the translation API's limit

/**
 * Layer 3: turn every Lark Original into a Relay, and withdraw the Relay when Lark withdraws. Layer
 * 7: then ask the Backend for the Full text in English and Update the Relay with it.
 */
class LarkListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recorder by lazy { Recorder(File(filesDir, "events.jsonl")) }
    private val translator by lazy { defaultTranslator(recorder::fallback) }
    private val userToken by lazy { defaultUserToken(this) }
    private val updates = HashMap<String, Job>() // in-flight Update per Original key
    private lateinit var manager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(RELAY_CHANNEL, "Relays", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ERRORS_CHANNEL,
                "Errors (debug)",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.i(
            TAG,
            "connected; active Lark notifications: ${activeNotifications.count { it.packageName == LARK }}",
        )
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
            val relaySender = translator.senderOf(preview.sender)
            val relay =
                buildRelay(
                    this@LarkListener,
                    sbn,
                    relayTitle,
                    relaySender,
                    preview.mention,
                    translator.englishOf(preview.message),
                )
            manager.notify(sbn.key, RELAY_ID, relay)
            val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            recorder.relayed(sbn.key, title, text, relayTitle, relayText)
            // Debug builds keep the Original next to the Relay for comparison while we
            // develop. Release builds cancel it (Max, 2026-08-25).
            if (!BuildConfig.DEBUG) cancelNotification(sbn.key)
            Log.i(TAG, "relayed key=${sbn.key} title=[$title] text=[$relayText]")
            // An uncut Preview already holds the whole message, so there is nothing to fetch:
            // 33 of 34 such Relays had a Full text identical to their Preview (Experiment 13).
            // Skipping them halves the API load, and with it the translate rate limit that
            // drives the ML Kit fallback. Recorded, so a soak can still price the exception.
            if (preview.truncated) {
                update(sbn, title, text, relayTitle, relaySender, preview)
            } else {
                recorder.skipped(sbn.key, "not-truncated")
            }
        }
    }

    /**
     * Layer 7: the Backend runs the Lookup and translates the Full text; the phone Updates the
     * Relay with it.
     */
    private suspend fun update(
        sbn: StatusBarNotification,
        title: String,
        text: String,
        relayTitle: String,
        relaySender: String,
        preview: Preview,
    ) {
        try {
            val answer =
                withContext(Dispatchers.IO) {
                    Backend.lookup(title, text, sbn.postTime, userToken.bearer())
                }
            if (answer.outcome != "found") {
                recorder.skipped(sbn.key, answer.reason)
                return
            }
            // No English from the Backend means Lark would not translate it there. The phone's own
            // Translator then tries Lark once more and falls back to ML Kit, marked `~`, as it
            // always has.
            val message =
                answer.english ?: translator.englishOf(answer.fullText.take(MAX_TRANSLATE_CHARS))
            val relay = buildRelay(this, sbn, relayTitle, relaySender, preview.mention, message)
            manager.notify(sbn.key, RELAY_ID, relay)
            val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            recorder.updated(sbn.key, answer.msgType, answer.fullText, relayText)
            Log.i(TAG, "updated key=${sbn.key} text=[$relayText]")
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
