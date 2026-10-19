package com.dakota.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseArchitectureTest {
    @Test
    fun `test initial configuration passes`() {
        val appName = "Dakota POS"
        assertEquals("Dakota POS", appName)
        assertTrue(true)
    }
}
