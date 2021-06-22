package ch.obermuhlner.kimage.math

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

internal class FunctionsTest {

    private val epsilon: Float = 1E-10f

    @Test
    fun testFloatArraySum() {
        assertEquals(Float.NaN, floatArrayOf().sum())
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f).sum(length = 0))

        assertEquals(6f, floatArrayOf(1f, 2f, 3f).sum())
        assertEquals(6f, floatArrayOf(10f, 1f, 2f, 3f, 20f).sum(1, 3))

        assertEquals(26f, floatArrayOf(10f, 1f, 2f, 3f, 20f).sum(offset = 1))
        assertEquals(13f, floatArrayOf(10f, 1f, 2f, 3f, 20f).sum(length = 3))

    }

    @Test
    fun testFloatArrayAverage() {
        assertEquals(Float.NaN, floatArrayOf().average())
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f).average(length = 0))

        assertEquals(6f / 3, floatArrayOf(1f, 2f, 3f).average())
        assertEquals(6f / 3, floatArrayOf(10f, 1f, 2f, 3f, 20f).average(1, 3))

        assertEquals(26f / 4, floatArrayOf(10f, 1f, 2f, 3f, 20f).average(offset = 1))
        assertEquals(13f / 3, floatArrayOf(10f, 1f, 2f, 3f, 20f).average(length = 3))
    }

    @Test
    fun testFloatArrayMedianInplace() {
        assertEquals(Float.NaN, floatArrayOf().medianInplace())
        assertEquals(Float.NaN, floatArrayOf(2f, 1f, 3f).medianInplace(length = 0))

        assertEquals(2f, floatArrayOf(2f, 1f, 3f).medianInplace())
        assertEquals(2.5f, floatArrayOf(2f, 4f, 3f, 1f).medianInplace())
        assertEquals(2f, floatArrayOf(10f, 1f, 2f, 3f, 20f).medianInplace(1, 3))
        assertEquals(2.5f, floatArrayOf(10f, 4f, 1f, 2f, 3f, 20f).medianInplace(1, 4))

        assertEquals(2.5f, floatArrayOf(10f, 3f, 1f, 2f, 20f).medianInplace(offset = 1))
        assertEquals(2f, floatArrayOf(10f, 2f, 1f, 3f, 20f).medianInplace(length = 3))
    }

    @Test
    fun testFloatArrayMedian() {
        assertEquals(Float.NaN, floatArrayOf().median())
        assertEquals(Float.NaN, floatArrayOf(2f, 1f, 3f).median(length = 0))

        assertEquals(2f, floatArrayOf(2f, 1f, 3f).median())
        assertEquals(2.5f, floatArrayOf(2f, 4f, 3f, 1f).median())
        assertEquals(2f, floatArrayOf(10f, 1f, 2f, 3f, 20f).median(1, 3))
        assertEquals(2.5f, floatArrayOf(10f, 4f, 1f, 2f, 3f, 20f).median(1, 4))

        assertEquals(2.5f, floatArrayOf(10f, 3f, 1f, 2f, 20f).median(offset = 1))
        assertEquals(2f, floatArrayOf(10f, 2f, 1f, 3f, 20f).median(length = 3))

        val array = floatArrayOf(2f, 1f, 3f)
        array.median()
        assertArrayEquals(floatArrayOf(2f, 1f, 3f), array, epsilon)
    }

    @Test
    fun testFloatArrayStddev() {
        assertEquals(Float.NaN, floatArrayOf().stddev())
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f, 4f).stddev(length = 0))

        assertEquals(sqrt(5f/4f), floatArrayOf(1f, 2f, 3f, 4f).stddev())
        assertEquals(sqrt(5f/4f), floatArrayOf(1f, 2f, 3f, 4f).stddev(StandardDeviation.Population))
        assertEquals(sqrt(5f/3f), floatArrayOf(1f, 2f, 3f, 4f).stddev(StandardDeviation.Sample))

        assertEquals(sqrt(5f/4f), floatArrayOf(10f, 1f, 2f, 3f, 4f, 20f).stddev(offset = 1, length = 4))
        assertEquals(sqrt(5f/4f), floatArrayOf(10f, 1f, 2f, 3f, 4f).stddev(offset = 1))
        assertEquals(sqrt(5f/4f), floatArrayOf(1f, 2f, 3f, 4f, 20f).stddev(length = 4))
    }

    @Test
    fun testFloatArraySigmaClipInplace() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).sigmaClipInplace(), epsilon)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClipInplace(iterations = 1), epsilon)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClipInplace(iterations = 2), epsilon)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClipInplace(iterations = 3), epsilon)
    }

    @Test
    fun testFloatArraySigmaClip() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).sigmaClip(), epsilon)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 1), epsilon)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 2), epsilon)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 3), epsilon)

        val array = floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f)
        array.sigmaClip()
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f), array, epsilon)
    }
}
