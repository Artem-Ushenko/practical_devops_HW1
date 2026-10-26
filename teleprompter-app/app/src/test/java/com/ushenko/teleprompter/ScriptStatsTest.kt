package com.ushenko.teleprompter

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptStatsTest {
    @Test
    fun countsWordsIgnoringPunctuationAndWhitespace() {
        assertEquals(0, ScriptStats.wordCount(""))
        assertEquals(0, ScriptStats.wordCount("  \n — ! "))
        assertEquals(4, ScriptStats.wordCount("Привіт,  світ!\nЦе тест."))
    }

    @Test
    fun estimatesDurationRoundingUp() {
        val text = List(130) { "слово" }.joinToString(" ")
        assertEquals(60, ScriptStats.estimatedSeconds(text))
        assertEquals(1, ScriptStats.estimatedSeconds("одне"))
        assertEquals(0, ScriptStats.estimatedSeconds(""))
    }

    @Test
    fun formatsDuration() {
        assertEquals("0:00", ScriptStats.formatDuration(0))
        assertEquals("1:05", ScriptStats.formatDuration(65))
        assertEquals("12:00", ScriptStats.formatDuration(720))
    }
}
