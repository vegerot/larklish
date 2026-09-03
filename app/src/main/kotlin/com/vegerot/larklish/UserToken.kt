package com.vegerot.larklish

import android.content.Context
import android.util.Log
import java.io.File
import org.json.JSONObject

private const val TAG = "UserToken"
private const val REFRESH_AHEAD_MS = 5 * 60 * 1000L // refresh 5 min early, like lark-cli

/**
 * Max's user access token for the Open API (Layer 5). The seed refresh token comes from
 * `tools/lark-token` via BuildConfig. Every refresh rotates the refresh token, so the live pair is
 * persisted in [file] (`filesDir/user-token.json`). A new seed (re-run of the tool after the chain
 * died) replaces the file. Access tokens last 2 h, refresh tokens 7 days sliding (Experiment 06
 * E4). Blocking: call on `Dispatchers.IO`.
 */
class UserToken(
    private val file: File,
    private val appId: String,
    private val appSecret: String,
    private val seed: String,
) {
    private var accessToken = ""
    private var refreshToken = seed
    private var expiresAt = 0L // epoch ms; 0 forces a refresh

    init {
        val saved = if (file.exists()) JSONObject(file.readText()) else null
        if (saved != null && saved.getString("seed") == seed.takeLast(16)) {
            accessToken = saved.getString("accessToken")
            refreshToken = saved.getString("refreshToken")
            expiresAt = saved.getLong("expiresAt")
        }
    }

    /** A valid access token. Refreshes when the current one is within 5 min of expiry. */
    @Synchronized
    fun bearer(): String {
        if (System.currentTimeMillis() >= expiresAt - REFRESH_AHEAD_MS) refresh()
        return accessToken
    }

    /** `POST authen/v2/oauth/token` with `grant_type=refresh_token`; rotates the refresh token. */
    @Synchronized
    fun refresh() {
        val json =
            LarkHttp.postJson(
                "/open-apis/authen/v2/oauth/token",
                JSONObject()
                    .put("grant_type", "refresh_token")
                    .put("refresh_token", refreshToken)
                    .put("client_id", appId)
                    .put("client_secret", appSecret),
            )
        accessToken = json.getString("access_token")
        refreshToken = json.getString("refresh_token")
        expiresAt = System.currentTimeMillis() + json.getLong("expires_in") * 1000
        file.writeText(
            JSONObject()
                .put("seed", seed.takeLast(16))
                .put("accessToken", accessToken)
                .put("refreshToken", refreshToken)
                .put("expiresAt", expiresAt)
                .toString()
        )
        Log.i(
            TAG,
            "refreshed: access token for ${json.getLong("expires_in")} s, refresh token …${refreshToken.takeLast(4)}",
        )
    }
}

fun defaultUserToken(context: Context) =
    UserToken(
        File(context.filesDir, "user-token.json"),
        BuildConfig.LARK_APP_ID,
        BuildConfig.LARK_APP_SECRET,
        BuildConfig.LARK_USER_REFRESH_TOKEN,
    )
