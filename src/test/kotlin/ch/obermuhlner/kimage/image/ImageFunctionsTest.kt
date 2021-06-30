package ch.obermuhlner.kimage.image

import org.junit.Assert.assertEquals
import org.junit.Test

internal class ImageFunctionsTest {

    private val epsilon = 1E-6

    @Test
    fun testValueIterable() {
        val image = MatrixImage(1, 1)
        image[0, 0, Channel.Red] = 0.1
        image[0, 0, Channel.Green] = 0.2
        image[0, 0, Channel.Blue] = 0.3

        val iterator = image.values().iterator()

        assertEquals(true, iterator.hasNext())
        assertEquals(0.1, iterator.next(), epsilon)

        assertEquals(true, iterator.hasNext())
        assertEquals(0.2, iterator.next(), epsilon)

        assertEquals(true, iterator.hasNext())
        assertEquals(0.3, iterator.next(), epsilon)

        assertEquals(false, iterator.hasNext())
    }
}