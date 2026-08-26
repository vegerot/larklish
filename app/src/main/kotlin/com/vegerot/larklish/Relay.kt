package com.vegerot.larklish

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification

const val RELAY_CHANNEL = "relay"

/**
 * The English notification Larklish posts in place of an Original.
 * Keeps the Original's tap intent, avatar and time. The Sender is romanized, never translated.
 */
fun buildRelay(
    context: Context,
    original: StatusBarNotification,
    title: String,
    preview: Preview,
    message: String,
): Notification {
    val n = original.notification
    val sender = romanize(preview.sender) + when (preview.mention) {
        Mention.YOU -> "@you"
        Mention.ALL -> "@all"
        null -> ""
    }
    val text = if (sender.isEmpty()) message else "$sender: $message"
    return Notification.Builder(context, RELAY_CHANNEL)
        .setSmallIcon(android.R.drawable.stat_notify_chat) // placeholder; see plan.md "App icon"
        .setLargeIcon(n.getLargeIcon())
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(Notification.BigTextStyle().bigText(text))
        .setContentIntent(n.contentIntent)
        .setWhen(n.`when`)
        .setShowWhen(true)
        .setAutoCancel(true)
        .build()
}
