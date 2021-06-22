package ch.obermuhlner.kimage.math

import kotlin.NoSuchElementException
import kotlin.math.sqrt

fun clamp(x: Double, min: Double, max: Double): Double {
    return when {
        x < min -> min
        x > max -> max
        else -> x
    }
}

fun clamp(x: Float, min: Float, max: Float): Float {
    return when {
        x < min -> min
        x > max -> max
        else -> x
    }
}

fun clamp(x: Int, min: Int, max: Int): Int {
    return when {
        x < min -> min
        x > max -> max
        else -> x
    }
}

private fun Iterator<Float>.sumAndCountFloat(): Pair<Float, Int> {
    var count = 0
    var sum = 0.0f
    for (value in this) {
        sum += value
        count++
    }

    return if (count == 0) Pair(Float.NaN, 0) else Pair(sum, count)
}

fun Iterator<Double>.sumAndCountDouble(): Pair<Double, Int> {
    var count = 0
    var sum = 0.0
    for (value in this) {
        sum += value
        count++
    }

    return if (count == 0) Pair(Double.NaN, 0) else Pair(sum, count)
}

fun Iterator<Float>.sum(): Float {
    return sumAndCountFloat().first
}

fun Iterator<Double>.sum(): Double {
    return sumAndCountDouble().first
}

fun Iterable<Float>.sum(): Float {
    return iterator().sum()
}

fun Iterable<Double>.sum(): Double {
    return iterator().sum()
}

fun FloatArray.sum(offset: Int = 0, length: Int = size-offset): Float {
    return ArrayFloatIterator(this, offset, length).sum()
}

fun DoubleArray.sum(offset: Int = 0, length: Int = size-offset): Double {
    return ArrayDoubleIterator(this, offset, length).sum()
}

fun Iterator<Float>.average(): Float {
    val (sum, count) = sumAndCountFloat()
    return sum / count
}

fun Iterable<Float>.average(): Float {
    val (sum, count) = iterator().sumAndCountFloat()
    return sum / count
}

fun FloatArray.average(offset: Int = 0, length: Int = size-offset): Float {
    return ArrayFloatIterator(this, offset, length).sum() / length
}

enum class StandardDeviation { Population, Sample }

fun Iterable<Float>.stddev(type: StandardDeviation = StandardDeviation.Population): Float {
    val (sum, count) = iterator().sumAndCountFloat()

    when (count) {
        0 -> return Float.NaN
        1 -> return 0f
    }

    val avg = sum / count

    var sumDeltaSquare = 0f
    for (value in iterator()) {
        val delta = value - avg
        sumDeltaSquare += delta * delta
    }

    val denom = when (type) {
        StandardDeviation.Population -> count
        StandardDeviation.Sample -> count - 1
    }
    return sqrt(sumDeltaSquare / denom)
}

fun Iterable<Double>.stddev(type: StandardDeviation = StandardDeviation.Population): Double {
    val (sum, count) = iterator().sumAndCountDouble()

    when (count) {
        0 -> return Double.NaN
        1 -> return 0.0
    }

    val avg = sum / count

    var sumDeltaSquare = 0.0
    for (value in iterator()) {
        val delta = value - avg
        sumDeltaSquare += delta * delta
    }

    val denom = when (type) {
        StandardDeviation.Population -> count
        StandardDeviation.Sample -> count - 1
    }
    return sqrt(sumDeltaSquare / denom)
}

fun FloatArray.stddev(type: StandardDeviation = StandardDeviation.Population, offset: Int = 0, length: Int = size-offset): Float {
    return ArrayFloatIterable(this, offset, length).stddev(type)
}

fun DoubleArray.stddev(type: StandardDeviation = StandardDeviation.Population, offset: Int = 0, length: Int = size-offset): Double {
    return ArrayDoubleIterable(this, offset, length).stddev(type)
}

fun FloatArray.medianInplace(offset: Int = 0, length: Int = size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }

    sort(offset, offset+length)

    val half = offset + length / 2
    return if (length % 2 == 0) {
        (this[half-1] + this[half]) / 2
    } else {
        this[half]
    }
}

fun DoubleArray.medianInplace(offset: Int = 0, length: Int = size-offset): Double {
    if (length == 0) {
        return Double.NaN
    }

    sort(offset, offset+length)

    val half = offset + length / 2
    return if (length % 2 == 0) {
        (this[half-1] + this[half]) / 2
    } else {
        this[half]
    }
}

fun MutableList<Float>.medianInplace(offset: Int = 0, length: Int = size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }

    subList(offset, offset+length).sort()

    val half = offset + length / 2
    return if (length % 2 == 0) {
        (this[half-1] + this[half]) / 2
    } else {
        this[half]
    }
}

fun MutableList<Double>.medianInplace(offset: Int = 0, length: Int = size-offset): Double {
    if (length == 0) {
        return Double.NaN
    }

    subList(offset, offset+length).sort()

    val half = offset + length / 2
    return if (length % 2 == 0) {
        (this[half-1] + this[half]) / 2
    } else {
        this[half]
    }
}

fun FloatArray.median(offset: Int = 0, length: Int = size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }

    val array = copyOfRange(offset, offset+length)
    return array.medianInplace()
}

fun DoubleArray.median(offset: Int = 0, length: Int = size-offset): Double {
    if (length == 0) {
        return Double.NaN
    }

    val array = copyOfRange(offset, offset+length)
    return array.medianInplace()
}

fun Iterable<Float>.median(): Float {
    return toMutableList().medianInplace()
}

fun Iterable<Double>.median(): Double {
    return toMutableList().medianInplace()
}

fun FloatArray.sigmaClipInplace(kappa: Float = 2f, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (FloatArray, Int, Int) -> Float = FloatArray::median): FloatArray {
    var currentLength = length

    for (i in 0 until iterations) {
        val sigma = stddev(standardDeviationType, offset, currentLength)
        val m = center(this, offset, currentLength)

        val low = m - kappa * sigma
        val high = m + kappa * sigma

        var targetLength = 0
        for (source in offset until (offset+currentLength)) {
            if (this[source] in low..high) {
                this[offset + targetLength++] = this[source]
            }
        }
        currentLength = targetLength
    }

    return copyOfRange(offset, offset+currentLength)
}

fun FloatArray.sigmaClip(kappa: Float = 2f, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (FloatArray, Int, Int) -> Float = FloatArray::median): FloatArray {
    val array = copyOfRange(offset, offset+length)

    return array.sigmaClipInplace(kappa, iterations, 0, length, standardDeviationType, center)
}

private class ArrayFloatIterator(private val array: FloatArray, private val offset: Int, private val length: Int) : FloatIterator() {
    private var index = offset
    override fun hasNext() = index < offset + length
    override fun nextFloat() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayDoubleIterator(private val array: DoubleArray, private val offset: Int, private val length: Int) : DoubleIterator() {
    private var index = offset
    override fun hasNext() = index < offset + length
    override fun nextDouble() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayFloatIterable(private val array: FloatArray, private val offset: Int, private val length: Int) : Iterable<Float> {
    override fun iterator(): Iterator<Float> = ArrayFloatIterator(array, offset, length)
}

private class ArrayDoubleIterable(private val array: DoubleArray, private val offset: Int, private val length: Int) : Iterable<Double> {
    override fun iterator(): Iterator<Double> = ArrayDoubleIterator(array, offset, length)
}
