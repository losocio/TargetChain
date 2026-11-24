package com.targetchain.targetchain.challenge

/**
 * Number of characters (code points) that changing [before] into [after] inserts: the length of the
 * changed region of [after], once the common prefix and suffix are removed.
 *
 * Typing inserts one character at a time; pasting, autofill, suggestions or dictation insert
 * several at once, which is how the password challenge tells them apart.
 */
fun insertedCharCount(before: CharSequence, after: CharSequence): Int {
    val maxCommon = minOf(before.length, after.length)
    var prefix = 0
    while (prefix < maxCommon && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    while (suffix < maxCommon - prefix &&
        before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
    ) {
        suffix++
    }
    val inserted = after.subSequence(prefix, after.length - suffix)
    return Character.codePointCount(inserted, 0, inserted.length)
}
