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
    val originalNotification = original.notification
    val sender = romanize(preview.sender) + when (preview.mention) {
        Mention.YOU -> "@you"
        Mention.ALL -> "@all"
        null -> ""
    }
    val text = if (sender.isEmpty()) message else "$sender: $message"
    return Notification.Builder(context, RELAY_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification) // 译鸟 silhouette; see plan.md "App icon"
        .setLargeIcon(originalNotification.getLargeIcon())
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(Notification.BigTextStyle().bigText(text))
        .setContentIntent(originalNotification.contentIntent)
        .setWhen(originalNotification.`when`)
        .setShowWhen(true)
        .setOnlyAlertOnce(true) // the Update (Layer 5) replaces the text without a second alert
        .setAutoCancel(true)
        .build()
}
