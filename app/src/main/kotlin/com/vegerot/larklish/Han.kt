package com.vegerot.larklish

// ICU-free on purpose: unit tests on the JVM use these, and Romanize.kt's ICU
// Transliterator only exists on the phone.

fun Char.isHan() = Character.isIdeographic(code)

/**
 * A bare Chinese personal name: nothing but Han, and short enough to be a name.
 * Only these romanize into something a reader can use (陈昱萌 → "Chen Yumeng").
 */
fun String.isHanName() = length in 2..4 && all { it.isHan() }
