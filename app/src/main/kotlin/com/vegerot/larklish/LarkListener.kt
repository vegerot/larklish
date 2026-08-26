package com.vegerot.larklish

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

private const val TAG = "LarkListener"
private const val LARK = "com.larksuite.suite"

/** Layer 1: log every Lark Original. No Relay yet. */
class LarkListener : NotificationListenerService() {

    override fun onListenerConnected() {
        Log.i(TAG, "connected; active Lark notifications: ${activeNotifications.count { it.packageName == LARK }}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != LARK) return
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        Log.i(
            TAG,
            "posted key=${sbn.key} title=[$title] text=[$text] textLen=${text?.length} " +
                "bigTextSame=${bigText == text} template=${extras.getString(Notification.EXTRA_TEMPLATE)} " +
                "contentIntent=${n.contentIntent != null} flags=0x${n.flags.toString(16)} " +
                "group=${n.group} channel=${n.channelId}",
        )
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
        reason: Int,
    ) {
        if (sbn.packageName != LARK) return
        Log.i(TAG, "removed key=${sbn.key} reason=$reason")
    }
}
