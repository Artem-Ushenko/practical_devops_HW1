package com.ushenko.teleprompter

/** Simple reading statistics for a script. */
object ScriptStats {
    /** Average speaking pace for on-camera talk, words per minute. */
    const val DEFAULT_WPM = 130

    fun wordCount(text: String): Int =
        text.split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }

    fun estimatedSeconds(text: String, wordsPerMinute: Int = DEFAULT_WPM): Int {
        require(wordsPerMinute > 0) { "wordsPerMinute must be positive" }
        val words = wordCount(text)
        return (words * 60 + wordsPerMinute - 1) / wordsPerMinute
    }

    fun formatDuration(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }
}
