package ch.obermuhlner.util

import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class SimpleTokenizerTest {
    @Test
    fun testTokenize() {
        assertEquals(listOf("alpha", "beta", "gamma"), SimpleTokenizer.tokenize("alpha beta gamma"))
        assertEquals(listOf("alpha", "beta"), SimpleTokenizer.tokenize("alpha   beta"))
        assertEquals(listOf("alpha", "beta b", "gamma"), SimpleTokenizer.tokenize("alpha \"beta b\" gamma"))
    }
}