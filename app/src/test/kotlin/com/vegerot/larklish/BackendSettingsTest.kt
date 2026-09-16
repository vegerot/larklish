package com.vegerot.larklish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendSettingsTest {
    @Test
    fun normalizesHttpsUrl() {
        assertEquals(
            "https://example.com/backend",
            BackendSettings.normalize("  https://example.com/backend/  "),
        )
    }

    @Test
    fun rejectsInvalidOrUnsafeUrls() {
        for (url in
            listOf(
                "http://example.com",
                "example.com",
                "https://",
                "https://user@example.com",
                "https://example.com/?token=a",
                "https://example.com/#part",
            )) {
            assertNull(url, BackendSettings.normalize(url))
        }
    }
}
