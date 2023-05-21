package ch.obermuhlner.util

import junit.framework.TestCase.assertEquals
import org.junit.Test

class WCSParserTest {
    @Test
    fun testParse() {
        val result = WCSParser.parse(
            listOf(
                "CTYPE1  = 'RA---TAN'           / first parameter RA  ,  projection TANgential   ",
                "CTYPE2='DEC--TAN'           / second parameter DEC,  projection TANgential",
                "CUNIT1  = 'deg     '           / Unit of coordinates            ",
                "CRPIX1  =  2.750500000000E+003 / X of reference pixel   ",
                "CRPIX2=1.650500000000E+003 / Y of reference pixel  "
            )
        )
        assertEquals("RA---TAN", result["CTYPE1"])
        assertEquals("DEC--TAN", result["CTYPE2"])
        assertEquals("deg     ", result["CUNIT1"])
        assertEquals("2.750500000000E+003", result["CRPIX1"])
        assertEquals("1.650500000000E+003", result["CRPIX2"])
    }
}