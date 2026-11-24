package com.targetchain.targetchain.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeLogicTest {

    @Test
    fun `typing or deleting one character inserts at most one`() {
        assertEquals(1, insertedCharCount("", "a"))
        assertEquals(1, insertedCharCount("ab", "abc"))
        assertEquals(1, insertedCharCount("ac", "abc"))
        assertEquals(1, insertedCharCount("aa", "aaa"))
        assertEquals(0, insertedCharCount("abc", "ab"))
        assertEquals(0, insertedCharCount("abc", ""))
    }

    @Test
    fun `replacing a selection with one character inserts one`() {
        assertEquals(1, insertedCharCount("abcd", "aXd"))
    }

    @Test
    fun `pasting or autofilling inserts several`() {
        assertEquals(6, insertedCharCount("", "secret"))
        assertEquals(2, insertedCharCount("a", "aaa"))
        assertEquals(3, insertedCharCount("ab", "aXYZb"))
        assertEquals(3, insertedCharCount("abcd", "aXYZd"))
    }

    @Test
    fun `an emoji counts as one character`() {
        assertEquals(1, insertedCharCount("a", "a😀"))
    }

    @Test
    fun `challenges are completed one by one`() {
        assertEquals(0, ChallengeProgress(total = 2, completed = 0).currentIndex)
        assertEquals(1, ChallengeProgress(total = 2, completed = 1).currentIndex)
        assertFalse(ChallengeProgress(total = 2, completed = 1).isFinished)
        assertNull(ChallengeProgress(total = 2, completed = 2).currentIndex)
        assertTrue(ChallengeProgress(total = 2, completed = 2).isFinished)
    }

    @Test
    fun `an alarm without challenges is never finished by them`() {
        assertNull(ChallengeProgress(total = 0, completed = 0).currentIndex)
        assertFalse(ChallengeProgress(total = 0, completed = 0).isFinished)
    }

    @Test
    fun `moving challenges reorders the list`() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.moved(1, 0))
        assertEquals(listOf("a", "c", "b"), list.moved(1, 2))
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(2, 3))
    }
}
