package ch.obermuhlner.kimage.math

import ch.obermuhlner.kimage.matrix.Matrix
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

internal class MathFunctionsTest {

    private val epsilonFloat: Float = 1E-8f
    private val epsilonDouble: Double = 1.0E-10


    @Test
    fun testFloatMix() {
        assertEquals(5.0f, mixLinear(5f, 7f, 0.0f), epsilonFloat)
        assertEquals(6.0f, mixLinear(5f, 7f, 0.5f), epsilonFloat)
        assertEquals(7.0f, mixLinear(5f, 7f, 1.0f), epsilonFloat)
    }

    @Test
    fun testDoubleMix() {
        assertEquals(5.0, mixLinear(5.0, 7.0, 0.0), epsilonDouble)
        assertEquals(6.0, mixLinear(5.0, 7.0, 0.5), epsilonDouble)
        assertEquals(7.0, mixLinear(5.0, 7.0, 1.0), epsilonDouble)
    }

    @Test
    fun testFloatArrayMin() {
        assertEquals(Float.NaN, floatArrayOf().min(), epsilonFloat)
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f).min(length = 0), epsilonFloat)

        assertEquals(1f, floatArrayOf(1f, 2f, 3f).min(), epsilonFloat)
        assertEquals(1f, floatArrayOf(10f, 1f, 2f, 3f, 20f).min(1, 3), epsilonFloat)

        assertEquals(1f, floatArrayOf(10f, 1f, 2f, 3f, 20f).min(offset = 1), epsilonFloat)
        assertEquals(1f, floatArrayOf(10f, 1f, 2f, 3f, 20f).min(length = 3), epsilonFloat)
    }

    @Test
    fun testFloatIterableMin() {
        assertEquals(1f, listOf(1f, 2f, 3f).min(), epsilonFloat)
    }

    @Test
    fun testFloatArrayMax() {
        assertEquals(Float.NaN, floatArrayOf().max(), epsilonFloat)
        assertEquals(Float.NaN, floatArrayOf(1f, 2f, 3f).max(length = 0), epsilonFloat)

        assertEquals(3f, floatArrayOf(1f, 2f, 3f).max(), epsilonFloat)
        assertEquals(3f, floatArrayOf(10f, 1f, 2f, 3f, 20f).max(1, 3), epsilonFloat)

        assertEquals(20f, floatArrayOf(10f, 1f, 2f, 3f, 20f).max(offset = 1), epsilonFloat)
        assertEquals(10f, floatArrayOf(10f, 1f, 2f, 3f, 20f).max(length = 3), epsilonFloat)
    }

    @Test
    fun testFloatIterableMax() {
        assertEquals(3f, listOf(1f, 2f, 3f).max(), epsilonFloat)
    }

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
    fun testDoubleArrayMin() {
        assertEquals(Double.NaN, doubleArrayOf().min(), epsilonDouble)
        assertEquals(Double.NaN, doubleArrayOf(1.0, 2.0, 3.0).min(length = 0), epsilonDouble)

        assertEquals(1.0, doubleArrayOf(1.0, 2.0, 3.0).min(), epsilonDouble)
        assertEquals(1.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).min(1, 3), epsilonDouble)

        assertEquals(1.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).min(offset = 1), epsilonDouble)
        assertEquals(1.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).min(length = 3), epsilonDouble)
    }

    @Test
    fun testDoubleIterableMin() {
        assertEquals(1f, listOf(1f, 2f, 3f).min(), epsilonFloat)
        assertEquals(1.0, Matrix.matrixOf(3, 1, 1.0, 2.0, 3.0).min(), epsilonDouble)
    }

    @Test
    fun testDoubleArrayMax() {
        assertEquals(Double.NaN, doubleArrayOf().max(), epsilonDouble)
        assertEquals(Double.NaN, doubleArrayOf(1.0, 2.0, 3.0).max(length = 0), epsilonDouble)

        assertEquals(3.0, doubleArrayOf(1.0, 2.0, 3.0).max(), epsilonDouble)
        assertEquals(3.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).max(1, 3), epsilonDouble)

        assertEquals(20.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).max(offset = 1), epsilonDouble)
        assertEquals(10.0, doubleArrayOf(10.0, 1.0, 2.0, 3.0, 20.0).max(length = 3), epsilonDouble)
    }

    @Test
    fun testDoubleIterableMax() {
        assertEquals(3f, listOf(1f, 2f, 3f).max(), epsilonFloat)
        assertEquals(3.0, Matrix.matrixOf(3, 1, 1.0, 2.0, 3.0).max(), epsilonDouble)
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
        assertEquals(6.0, Matrix.matrixOf(3, 1, 1.0, 2.0, 3.0).sum(), epsilonDouble)
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
    fun testFloatIterableAverage() {
        assertEquals(2f, listOf(1f, 2f, 3f).average(), epsilonFloat)
    }

    @Test
    fun testDoubleIterableAverage() {
        assertEquals(2.0, listOf(1.0, 2.0, 3.0).average(), epsilonDouble)
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
    fun testFloatIterableFastMedian() {
        assertEquals(Float.NaN, listOf<Float>().fastMedian(), epsilonFloat)
        assertEquals(Float.NaN, listOf<Float>().fastMedian(0.0f, 1000.0f), epsilonFloat)

        val data = mutableListOf<Float>()
        for (value in 0 .. 100) {
            data.add(value.toFloat())
        }
        for (value in 0 .. 10) {
            data.add(200.0f)
        }
        for (value in 0 .. 100) {
            data.add(1000.0f)
        }

        assertEquals(200.0f, data.fastMedian(), 10.0f)
        assertEquals(200.0f, data.fastMedian(0.0f, 1000.0f), 10.0f)
    }

    @Test
    fun testDoubleIterableFastMedian() {
        assertEquals(Double.NaN, listOf<Double>().fastMedian(), epsilonDouble)
        assertEquals(Double.NaN, listOf<Double>().fastMedian(0.0, 1000.0), epsilonDouble)

        val data = mutableListOf<Double>()
        for (value in 0 .. 100) {
            data.add(value.toDouble())
        }
        for (value in 0 .. 10) {
            data.add(200.0)
        }
        for (value in 0 .. 100) {
            data.add(1000.0)
        }

        assertEquals(200.0, data.fastMedian(), 10.0)
        assertEquals(200.0, data.fastMedian(0.0, 1000.0), 10.0)
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
    fun testFloatArraySigmaClip() {
        assertArrayEquals(floatArrayOf(), floatArrayOf().sigmaClip(), epsilonFloat)
        assertArrayEquals(floatArrayOf(3f), floatArrayOf(3f).sigmaClip(), epsilonFloat)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).sigmaClip(), epsilonFloat)

        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 1), epsilonFloat)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 20f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 2), epsilonFloat)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f, 20f, 50f, 60f).sigmaClip(iterations = 3), epsilonFloat)

        assertArrayEquals(floatArrayOf(1f), floatArrayOf(1f, 60f).sigmaClip(kappa = 0.1f), epsilonFloat)
        assertArrayEquals(floatArrayOf(), floatArrayOf(1f, 60f).sigmaClip(kappa = 0.1f, keepLast = false), epsilonFloat)

        val array = floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f)
        array.sigmaClip()
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f), array, epsilonFloat)
    }

    @Test
    fun testDoubleArraySigmaClip() {
        assertArrayEquals(doubleArrayOf(), doubleArrayOf().sigmaClip(), epsilonDouble)
        assertArrayEquals(doubleArrayOf(3.0), doubleArrayOf(3.0).sigmaClip(), epsilonDouble)

        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0, 90.0, 91.0).sigmaClip(), epsilonDouble)

        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 20.0, 50.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0, 20.0, 50.0, 60.0).sigmaClip(iterations = 1), epsilonDouble)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 20.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0, 20.0, 50.0, 60.0).sigmaClip(iterations = 2), epsilonDouble)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0, 20.0, 50.0, 60.0).sigmaClip(iterations = 3), epsilonDouble)

        assertArrayEquals(doubleArrayOf(1.0), doubleArrayOf(1.0, 60.0).sigmaClip(kappa = 0.1), epsilonDouble)
        assertArrayEquals(doubleArrayOf(), doubleArrayOf(1.0, 60.0).sigmaClip(kappa = 0.1, keepLast = false), epsilonDouble)

        val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 90.0, 91.0)
        array.sigmaClip()
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 90.0, 91.0), array, epsilonDouble)
    }

    @Test
    fun testFloatArrayWinsorizeInplace() {
        assertArrayEquals(floatArrayOf(2.5f, 2.5f, 3f, 4f, 10f, 10f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).winsorizeInplace(2.5f, 10f), epsilonFloat)
    }

    @Test
    fun testDoubleArrayWinsorizeInplace() {
        assertArrayEquals(doubleArrayOf(2.5, 2.5, 3.0, 4.0, 10.0, 10.0), doubleArrayOf(1.0, 2.0, 3.0, 4.0, 90.0, 91.0).winsorizeInplace(2.5, 10.0), epsilonDouble)
    }

    @Test
    fun testFloatArrayWinsorizeSigmaInplace() {
        val array = floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f)
        val median = array.median()
        val sigma = array.stddev()
        val high = median + sigma
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, high, high), array.sigmaWinsorizeInplace(1.0f), epsilonFloat)
    }

    @Test
    fun testDoubleArrayWinsorizeSigmaInplace() {
        val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 90.0, 91.0)
        val median = array.median()
        val sigma = array.stddev()
        val high = median + sigma
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0, high, high), array.sigmaWinsorizeInplace(1.0), epsilonDouble)
    }

    @Test
    fun testFloatArrayHuberWinsorizeInplace() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 24.780285f, 24.780285f), floatArrayOf(1f, 2f, 3f, 4f, 90f, 91f).huberWinsorizeInplace(), epsilonFloat)
    }

}
