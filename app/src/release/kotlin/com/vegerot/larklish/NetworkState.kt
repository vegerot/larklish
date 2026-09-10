package com.vegerot.larklish

import android.content.Context
import org.json.JSONObject

/** Release builds neither request diagnostic permissions nor collect network state. */
internal class NetworkState(@Suppress("UNUSED_PARAMETER") context: Context) : AutoCloseable {
    fun snapshot(): JSONObject? = null

    override fun close() = Unit
}
