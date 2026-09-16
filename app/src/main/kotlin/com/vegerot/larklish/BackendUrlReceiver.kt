package com.vegerot.larklish

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Shell-only shortcut: adb shell am broadcast -n com.vegerot.larklish/.BackendUrlReceiver --es
 * backend_url URL
 */
class BackendUrlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val input = intent.getStringExtra("backend_url")
        if (input != null && BackendSettings(context).save(input)) {
            resultCode = Activity.RESULT_OK
            resultData = BackendSettings(context).url
        } else {
            resultCode = Activity.RESULT_CANCELED
            resultData = "Expected an HTTPS backend_url without a query or fragment"
        }
    }
}
