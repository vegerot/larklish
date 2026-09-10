package com.vegerot.larklish

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkStateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun recordsTheNetworkAtEachEventIncludingAnUpdateFailure() {
        val file = File.createTempFile("network-record", ".jsonl", context.cacheDir)
        try {
            var network = JSONObject("""{"vpn":false,"wifiConnected":true,"wifiSsid":"Office"}""")
            val recorder = Recorder(file) { network }
            recorder.relayed("key", "group", "Sender: ...", "group", "Sender: ...", true)
            network = JSONObject("""{"vpn":true,"wifiConnected":false,"wifiSsid":null}""")
            recorder.updated("key", "text", "Full text", "Sender: Full text", "https://backend")
            network = JSONObject("""{"vpn":false,"wifiConnected":true,"wifiSsid":null}""")
            recorder.skipped("key", "error: Read timed out")

            val events = recorder.readAll()
            assertEquals("Office", events[0].getJSONObject("network").getString("wifiSsid"))
            assertTrue(events[0].getBoolean("truncated"))
            assertTrue(events[1].getJSONObject("network").getBoolean("vpn"))
            assertFalse(events[1].getJSONObject("network").getBoolean("wifiConnected"))
            assertEquals("https://backend", events[1].getString("backend"))
            assertTrue(events[2].getJSONObject("network").getBoolean("wifiConnected"))
            assertTrue(events[2].getJSONObject("network").isNull("wifiSsid"))
            assertEquals("error: Read timed out", events[2].getString("reason"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun wifiCallbackRecordsThePhonesSsid() {
        // Pass the phone's current SSID with -e expectedSsid, after granting debug location access.
        // Without that input this environment-dependent check has no known network to compare.
        val expected = InstrumentationRegistry.getArguments().getString("expectedSsid")
        assumeNotNull(expected)
        NetworkState(context).use { state ->
            val deadline = SystemClock.elapsedRealtime() + 5_000
            while (
                state.snapshot().isNull("wifiSsid") && SystemClock.elapsedRealtime() < deadline
            ) {
                SystemClock.sleep(50)
            }
            val snapshot = state.snapshot()
            assertTrue(snapshot.getBoolean("wifiConnected"))
            assertEquals(expected, snapshot.getString("wifiSsid"))
        }
    }
}
