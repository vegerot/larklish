package com.vegerot.larklish

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File
import java.util.UUID
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

/**
 * Layer 3: turn every Lark Original into a Relay, and withdraw the Relay when Lark withdraws. Layer
 * 7: then ask the Backend to improve every Han Preview, with Full text for cut Previews.
 */
class LarkListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var networkState: NetworkState
    private val recorder by lazy {
        Recorder(File(filesDir, "events.jsonl"), networkState::snapshot)
    }
    private val translator by lazy { defaultTranslator() }
    private val userToken by lazy { defaultUserToken(this) }
    private val updates = HashMap<String, Job>() // in-flight Update per Original key
    private lateinit var manager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        networkState = NetworkState(this)
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
        networkState.close()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.i(
            TAG,
            "connected; active Lark notifications: ${activeNotifications.count { it.packageName == LARK }}",
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val receivedAt = System.nanoTime()
        if (sbn.packageName != LARK) return
        val n = sbn.notification
        // Android's autogroup summary for Lark: no title, no text (Experiment 01).
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        val preview = Preview.parse(text)
        val timing = FlowTiming()
        val flowId = UUID.randomUUID().toString()
        // A translate failure (no model, no network) would crash the service. The model is on the
        // phone after the first Layer 2 run, so this is unlikely; Android restarts the service.
        // Lark reuses one key per chat: a newer Original on the same key makes the older
        // Update wrong, so cancel it (plan.md Layer 5).
        updates.remove(sbn.key)?.cancel()
        updates[sbn.key] =
            scope.launch(timing) {
                var outcome = "canceled"
                var firstRelayAt = 0L
                try {
                    val relayTitle = timing.measure("preview.title") { translator.englishOf(title) }
                    val relaySender =
                        timing.measure("preview.sender") { translator.senderOf(preview.sender) }
                    val relayMessage =
                        timing.measure("preview.message") { translator.englishOf(preview.message) }
                    val relay =
                        buildRelay(
                            this@LarkListener,
                            sbn,
                            relayTitle,
                            relaySender,
                            preview.mention,
                            relayMessage,
                        )
                    manager.notify(sbn.key, RELAY_ID, relay)
                    firstRelayAt = System.nanoTime()
                    timing.add("original_to_relay", (firstRelayAt - receivedAt) / 1_000_000)
                    val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                    recorder.relayed(
                        sbn.key,
                        title,
                        text,
                        relayTitle,
                        relayText,
                        preview.truncated,
                        flowId,
                    )
                    // Debug builds keep the Original next to the Relay for comparison while we
                    // develop. Release builds cancel it (Max, 2026-08-25).
                    if (!BuildConfig.DEBUG) cancelNotification(sbn.key)
                    Log.i(TAG, "relayed key=${sbn.key} title=[$title] text=[$relayText]")
                    // A complete Latin message needs no better translation. Title and Sender do
                    // not affect this decision: the Backend is for the message the notification
                    // is about. A cut Preview still needs Full text even when its visible part is
                    // Latin.
                    if (preview.needsBackend()) {
                        outcome =
                            update(
                                sbn,
                                title,
                                text,
                                relayTitle,
                                relaySender,
                                preview,
                                flowId,
                                timing,
                            )
                    } else {
                        recorder.skipped(sbn.key, "complete-no-han", flowId = flowId)
                        outcome = "complete-no-han"
                    }
                } finally {
                    if (firstRelayAt != 0L) {
                        timing.add(
                            "relay_to_outcome",
                            (System.nanoTime() - firstRelayAt) / 1_000_000,
                        )
                    }
                    recorder.timing(
                        sbn.key,
                        flowId,
                        outcome,
                        timing.snapshot(),
                        (System.nanoTime() - receivedAt) / 1_000_000,
                    )
                }
            }
    }

    /**
     * Layer 7: the Backend translates a complete Preview directly, or runs the Lookup and
     * translates Full text for a cut Preview. The phone Updates the Relay with it.
     */
    private suspend fun update(
        sbn: StatusBarNotification,
        title: String,
        text: String,
        relayTitle: String,
        relaySender: String,
        preview: Preview,
        flowId: String,
        timing: FlowTiming,
    ): String {
        try {
            val answer =
                withContext(Dispatchers.IO) {
                    Backend.lookup(
                        this@LarkListener,
                        title,
                        text,
                        sbn.postTime,
                        if (preview.truncated) {
                            timing.measureBlocking("user.token") { userToken.bearer(timing) }
                        } else null,
                        flowId,
                        timing,
                    )
                }
            timing.backend(answer.timings)
            if (answer.outcome != "found") {
                val reason =
                    if (answer.outcome == "translation-failed") {
                        "translation-failed:${answer.reason}"
                    } else answer.reason
                recorder.skipped(sbn.key, reason, answer.backend, flowId)
                return answer.outcome
            }
            val message = checkNotNull(answer.english)
            val relay =
                buildRelay(
                    this,
                    sbn,
                    answer.title ?: relayTitle,
                    answer.sender ?: relaySender,
                    preview.mention,
                    message,
                )
            manager.notify(sbn.key, RELAY_ID, relay)
            val relayText = relay.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            recorder.updated(
                sbn.key,
                answer.source,
                answer.msgType,
                answer.message,
                relayText,
                answer.backend,
                answer.failures,
                flowId,
            )
            Log.i(TAG, "updated key=${sbn.key} via ${answer.backend} text=[$relayText]")
            return "updated-${answer.source}"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "update failed key=${sbn.key}: $e")
            recorder.skipped(sbn.key, "error: $e", flowId = flowId)
            if (BuildConfig.DEBUG) manager.notify(ERROR_ID, errorNotification(e))
            return "error"
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
