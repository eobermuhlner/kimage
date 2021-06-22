package ch.obermuhlner.kimage.math

import ch.obermuhlner.kimage.matrix.Matrix
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

internal class FunctionsTest {

    private val epsilonFloat: Float = 1E-8f
    private val epsilonDouble: Double = 1.0E-10

    @Test
    fun testFloatArraySum() {
        assertEquals(Float.NaN, floatArrayOf().sum(), epsilonFloat)
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f).sum(length = 0), epsilonFloat)

        assertEquals(6f, floatArrayOf(1f, 2f, 3f).sum(), epsilonFloat)
        assertEquals(6f, floatArrayOf(10f, 1f, 2f, 3f, 20f).sum(1, 3), epsilonFloat)

        assertEquals(26f, floatArrayOf(10f, 1f, 2f, 3f, 20f).sum(offset = 1), epsilonFloat)
        assertEquals(13f, floatArrayOf(10f, 1f, 2f, 3f, 20f).sum(length = 3), epsilonFloat)
    }

    @Test
    fun testFloatIterableSum() {
        assertEquals(6f, listOf(1f, 2f, 3f).sum(), epsilonFloat)
    }

    @Test
    fun testDoubleArraySum() {
        assertEquals(Double.NaN, doubleArrayOf().sum(), epsilonDouble)
        assertEquals(Double.NaN, doubleArrayOf(1.0, 2.0, 3.0).sum(length = 0), epsilonDouble)

        assertEquals(6.0, doubleArrayOf(1.0, 2.0, 3.0).sum(), epsilonDouble)
        assertEquals(6.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).sum(1, 3), epsilonDouble)

        assertEquals(26.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).sum(offset = 1), epsilonDouble)
        assertEquals(13.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).sum(length = 3), epsilonDouble)
    }

    @Test
    fun testDoubleIterableSum() {
        assertEquals(6.0, listOf(1.0, 2.0, 3.0).sum(), epsilonDouble)
        assertEquals(6.0, Matrix.matrixOf(1, 3, 1.0, 2.0, 3.0).sum(), epsilonDouble)
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
    fun testFloatMutableListMedianInplace() {
        assertEquals(Float.NaN, mutableListOf<Float>().medianInplace())
        assertEquals(Float.NaN, mutableListOf(2f, 1f, 3f).medianInplace(length = 0))

        assertEquals(2f, mutableListOf(2f, 1f, 3f).medianInplace())
        assertEquals(2.5f, mutableListOf(2f, 4f, 3f, 1f).medianInplace())
        assertEquals(2f, mutableListOf(10f, 1f, 2f, 3f, 20f).medianInplace(1, 3))
        assertEquals(2.5f, mutableListOf(10f, 4f, 1f, 2f, 3f, 20f).medianInplace(1, 4))

        assertEquals(2.5f, mutableListOf(10f, 3f, 1f, 2f, 20f).medianInplace(offset = 1))
        assertEquals(2f, mutableListOf(10f, 2f, 1f, 3f, 20f).medianInplace(length = 3))
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
        assertArrayEquals(floatArrayOf(2f, 1f, 3f), array, epsilonFloat)
    }

    @Test
    fun testFloatIterableMedian() {
        assertEquals(Float.NaN, listOf<Float>().median(), epsilonFloat)

        assertEquals(2f, listOf(2f, 1f, 3f).median(), epsilonFloat)
        assertEquals(2.5f, listOf(2f, 4f, 3f, 1f).median(), epsilonFloat)

        val list = mutableListOf(2f, 1f, 3f)
        list.median()
        assertEquals(listOf(2f, 1f, 3f), list)
    }

    @Test
    fun testDoubleArrayMedianInplace() {
        assertEquals(Double.NaN, doubleArrayOf().medianInplace(), epsilonDouble)
        assertEquals(Double.NaN, doubleArrayOf(2.0, 1.0, 3.0).medianInplace(length = 0), epsilonDouble)

        assertEquals(2.0, doubleArrayOf(2.0, 1.0, 3.0).medianInplace(), epsilonDouble)
        assertEquals(2.5, doubleArrayOf(2.0, 4.0, 3.0, 1.0).medianInplace(), epsilonDouble)
        assertEquals(2.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).medianInplace(1, 3), epsilonDouble)
        assertEquals(2.5, doubleArrayOf(10.0, 4.0, 1.0, 2.0, 3.0, 20.0).medianInplace(1, 4), epsilonDouble)

        assertEquals(2.5, doubleArrayOf(10.0, 3.0, 1.0, 2.0, 20.0).medianInplace(offset = 1), epsilonDouble)
        assertEquals(2.0, doubleArrayOf(10.0, 2.0, 1.0, 3.0, 20.0).medianInplace(length = 3), epsilonDouble)
    }

    @Test
    fun testDoubleMutableListMedianInplace() {
        assertEquals(Double.NaN, mutableListOf<Double>().medianInplace(), epsilonDouble)
        assertEquals(Double.NaN, mutableListOf(2.0, 1.0, 3.0).medianInplace(length = 0), epsilonDouble)

        assertEquals(2.0, mutableListOf(2.0, 1.0, 3.0).medianInplace(), epsilonDouble)
        assertEquals(2.5, mutableListOf(2.0, 4.0, 3.0, 1.0).medianInplace(), epsilonDouble)
        assertEquals(2.0, mutableListOf(10.0, 1.0, 2.0, 3.0, 20.0).medianInplace(1, 3), epsilonDouble)
        assertEquals(2.5, mutableListOf(10.0, 4.0, 1.0, 2.0, 3.0, 20.0).medianInplace(1, 4), epsilonDouble)

        assertEquals(2.5, mutableListOf(10.0, 3.0, 1.0, 2.0, 20.0).medianInplace(offset = 1), epsilonDouble)
        assertEquals(2.0, mutableListOf(10.0, 2.0, 1.0, 3.0, 20.0).medianInplace(length = 3), epsilonDouble)
    }

    @Test
    fun testDoubleArrayMedian() {
        assertEquals(Double.NaN, doubleArrayOf().median(), epsilonDouble)
        assertEquals(Double.NaN, doubleArrayOf(2.0, 1.0, 3.0).median(length = 0), epsilonDouble)

        assertEquals(2.0, doubleArrayOf(2.0, 1.0, 3.0).median(), epsilonDouble)
        assertEquals(2.5, doubleArrayOf(2.0, 4.0, 3.0, 1.0).median(), epsilonDouble)
        assertEquals(2.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).median(1, 3), epsilonDouble)
        assertEquals(2.5, doubleArrayOf(10.0, 4.0, 1.0, 2.0, 3.0, 20.0).median(1, 4), epsilonDouble)

        assertEquals(2.5, doubleArrayOf(10.0, 3.0, 1.0, 2.0, 20.0).median(offset = 1), epsilonDouble)
        assertEquals(2.0, doubleArrayOf(10.0, 2.0, 1.0, 3.0, 20.0).median(length = 3), epsilonDouble)

        val array = doubleArrayOf(2.0, 1.0, 3.0)
        array.median()
        assertArrayEquals(doubleArrayOf(2.0, 1.0, 3.0), array, epsilonDouble)
    }

    @Test
    fun testDoubleIterableMedian() {
        assertEquals(Double.NaN, listOf<Double>().median(), epsilonDouble)

        assertEquals(2.0, listOf(2.0, 1.0, 3.0).median(), epsilonDouble)
        assertEquals(2.5, listOf(2.0, 4.0, 3.0, 1.0).median(), epsilonDouble)

        assertEquals(2.5, Matrix.matrixOf(2, 2, 2.0, 4.0, 3.0, 1.0).median(), epsilonDouble)

        val list = mutableListOf(2f, 1f, 3f)
        list.median()
        assertEquals(listOf(2f, 1f, 3f), list)
    }

    @Test
    fun testFloatArrayStddev() {
        assertEquals(Float.NaN, floatArrayOf().stddev(), epsilonFloat)
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f, 4f).stddev(length = 0), epsilonFloat)

        assertEquals(0f, floatArrayOf(3f).stddev(), epsilonFloat)

        assertEquals(sqrt(5f/4f), floatArrayOf(1f, 2f, 3f, 4f).stddev(), epsilonFloat)
        assertEquals(sqrt(5f/4f), floatArrayOf(1f, 2f, 3f, 4f).stddev(StandardDeviation.Population), epsilonFloat)
        assertEquals(sqrt(5f/3f), floatArrayOf(1f, 2f, 3f, 4f).stddev(StandardDeviation.Sample), epsilonFloat)

        assertEquals(sqrt(5f/4f), floatArrayOf(10f, 1f, 2f, 3f, 4f, 20f).stddev(offset = 1, length = 4), epsilonFloat)
        assertEquals(sqrt(5f/4f), floatArrayOf(10f, 1f, 2f, 3f, 4f).stddev(offset = 1), epsilonFloat)
        assertEquals(sqrt(5f/4f), floatArrayOf(1f, 2f, 3f, 4f, 20f).stddev(length = 4), epsilonFloat)
    }

    @Test
    fun testDoubleArrayStddev() {
        assertEquals(Double.NaN, doubleArrayOf().stddev(), epsilonDouble)
        assertEquals(Double.NaN, doubleArrayOf(1.0, 2.0, 3.0, 4.0).stddev(length = 0), epsilonDouble)

        assertEquals(0.0, doubleArrayOf(3.0).stddev(), epsilonDouble)

        assertEquals(sqrt(5.0/4.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0).stddev(), epsilonDouble)
        assertEquals(sqrt(5.0/4.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0).stddev(StandardDeviation.Population), epsilonDouble)
        assertEquals(sqrt(5.0/3.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0).stddev(StandardDeviation.Sample), epsilonDouble)

        assertEquals(sqrt(5.0/4.0), doubleArrayOf(10.0, 1.0, 2.0, 3.0, 4.0, 20.0).stddev(offset = 1, length = 4), epsilonDouble)
        assertEquals(sqrt(5.0/4.0), doubleArrayOf(10.0, 1.0, 2.0, 3.0, 4.0).stddev(offset = 1), epsilonDouble)
        assertEquals(sqrt(5.0/4.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0, 20.0).stddev(length = 4), epsilonDouble)
    }

    @Test
    fun testFloatIterableStddev() {
        assertEquals(sqrt(5f/4f), listOf(1f, 2f, 3f, 4f).stddev())
    }

    @Test
    fun testDoubleIterableStddev() {
        assertEquals(sqrt(5.0/4.0), listOf(1.0, 2.0, 3.0, 4.0).stddev(), epsilonDouble)
        assertEquals(sqrt(5.0/4.0), Matrix.matrixOf(2, 2, 1.0, 2.0, 3.0, 4.0).stddev(), epsilonDouble)
    }

    @Test
    fun testFloatArraySigmaClipInplace() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).sigmaClipInplace(), epsilonFloat)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClipInplace(iterations = 1), epsilonFloat)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClipInplace(iterations = 2), epsilonFloat)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClipInplace(iterations = 3), epsilonFloat)
    }

    @Test
    fun testFloatArraySigmaClip() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).sigmaClip(), epsilonFloat)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 1), epsilonFloat)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 2), epsilonFloat)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 3), epsilonFloat)

        val array = floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f)
        array.sigmaClip()
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f), array, epsilonFloat)
    }
}
