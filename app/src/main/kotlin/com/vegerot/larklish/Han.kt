package com.vegerot.larklish

// ICU-free on purpose: unit tests on the JVM use these, and Romanize.kt's ICU
// Transliterator only exists on the phone.

fun Char.isHan() = Character.isIdeographic(code)

/** True when [this] holds at least one Han character — the only text worth translating. */
fun String.hasHan() = any { it.isHan() }
