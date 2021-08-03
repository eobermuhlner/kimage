package ch.obermuhlner.kimage.math

import kotlin.NoSuchElementException
import kotlin.math.abs
import kotlin.math.sqrt

fun Float.finiteOrElse(default: Float = 0f) = if (this.isFinite()) this else default

fun Double.finiteOrElse(default: Double = 0.0) = if (this.isFinite()) this else default

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

fun smoothstep(x0: Double, x1: Double, a: Double): Double {
    val x = clamp((a - x0) / (x1 - x0), 0.0, 1.0)
    return x * x * (3.0 - 2.0 * x);
}

fun smootherstep(x0: Double, x1: Double, a: Double): Double {
    val x = clamp((a - x0) / (x1 - x0), 0.0, 1.0)
    return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
}

fun mixLinear(x0: Double, x1: Double, a: Double): Double {
    return (1.0 - a) * x0 + a * x1
}

fun mixLinear(x0: Float, x1: Float, a: Float): Float {
    return (1.0f - a) * x0 + a * x1
}

fun mixBilinear(x00: Double, x01: Double, x10: Double, x11: Double, ax: Double, ay: Double): Double {
    return mixLinear(mixLinear(x00, x10, ax), mixLinear(x01, x11, ax), ay)
}

fun mixCubicHermite(x00: Double, x01: Double, x10: Double, x11: Double, t: Double): Double {
    val a = -x00 / 2.0 + 3.0 * x01 / 2.0 - 3.0 * x10 / 2.0 + x11 / 2.0
    val b = x00 - 5.0 * x01 / 2.0 + 2.0 * x10 - x11 / 2.0
    val c = -x00 / 2.0 + x10 / 2.0
    return a * t * t * t + b * t * t + c * t + x01
}

private fun <T, U> Iterator<T>.reduceAndCount(initial: U, empty: U, accumulator: (U, T) -> U): Pair<U, Int> {
    var result = initial
    var count = 0

    for (value in this) {
        result = accumulator.invoke(result, value)
        count++
    }

    return if (count == 0) Pair(empty, 0) else Pair(result, count)
}

fun <T, U> Iterator<T>.reduce(initial: U, empty: U, accumulator: (U, T) -> U): U {
    return reduceAndCount(initial, empty, accumulator).first
}

fun Iterator<Float>.min(): Float {
    return reduce(Float.MAX_VALUE, Float.NaN) { a, b -> kotlin.math.min(a, b) }
}

fun Iterator<Double>.min(): Double {
    return reduce(Double.MAX_VALUE, Double.NaN) { a, b -> kotlin.math.min(a, b) }
}

fun FloatArray.min(offset: Int = 0, length: Int = size-offset): Float {
    return ArrayFloatIterator(this, offset, length).min()
}

fun DoubleArray.min(offset: Int = 0, length: Int = size-offset): Double {
    return ArrayDoubleIterator(this, offset, length).min()
}

fun Iterable<Float>.min(): Float {
    return iterator().min()
}

fun Iterable<Double>.min(): Double {
    return iterator().min()
}

fun Iterator<Float>.max(): Float {
    return reduce(Float.MIN_VALUE, Float.NaN) { a, b -> kotlin.math.max(a, b) }
}

fun Iterator<Double>.max(): Double {
    return reduce(Double.MIN_VALUE, Double.NaN) { a, b -> kotlin.math.max(a, b) }
}

fun FloatArray.max(offset: Int = 0, length: Int = size-offset): Float {
    return ArrayFloatIterator(this, offset, length).max()
}

fun DoubleArray.max(offset: Int = 0, length: Int = size-offset): Double {
    return ArrayDoubleIterator(this, offset, length).max()
}

fun Iterable<Float>.max(): Float {
    return iterator().max()
}

fun Iterable<Double>.max(): Double {
    return iterator().max()
}

private fun Iterator<Float>.sumAndCountFloat(): Pair<Float, Int> {
    return reduceAndCount(0f, Float.NaN) { a, b -> a + b }
}

fun Iterator<Double>.sumAndCountDouble(): Pair<Double, Int> {
    return reduceAndCount(0.0, Double.NaN) { a, b -> a + b }
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

fun Iterator<Double>.average(): Double {
    val (sum, count) = sumAndCountDouble()
    return sum / count
}

fun Iterable<Float>.average(): Float {
    return iterator().average()
}

fun Iterable<Double>.average(): Double {
    return iterator().average()
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

fun Iterable<Float>.fastMedian(binCount: Int = 100): Float {
    val min = min()
    val max = max()
    return fastMedian(min, max, binCount)
}

fun Iterable<Float>.fastMedian(min: Float, max: Float, binCount: Int = 100): Float {
    if (!this.iterator().hasNext()) {
        return Float.NaN;
    }

    val histogram = Histogram(binCount)
    for(value in this) {
        histogram.add(((value - min) / (max - min)).toDouble())
    }
    return (histogram.estimateMedian() * (max - min) + min).toFloat()
}

fun Iterable<Double>.fastMedian(binCount: Int = 100): Double {
    val min = min()
    val max = max()
    return fastMedian(min, max, binCount)
}

fun Iterable<Double>.fastMedian(min: Double, max: Double, binCount: Int = 100): Double {
    if (!this.iterator().hasNext()) {
        return Double.NaN;
    }

    val histogram = Histogram(binCount)
    for(value in this) {
        histogram.add((value - min) / (max - min))
    }
    return histogram.estimateMedian() * (max - min) + min
}

fun FloatArray.sigmaClip(kappa: Float = 2f, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (array: FloatArray, offset: Int, length: Int) -> Float = FloatArray::median, keepLast: Boolean = true, histogram: Histogram? = null): FloatArray {
    val array = copyOfRange(offset, offset+length)
    val clippedLength = array.sigmaClipInplace(kappa, iterations, 0, length, standardDeviationType, center, keepLast, histogram)
    return array.copyOfRange(0, clippedLength)
}

fun DoubleArray.sigmaClip(kappa: Double = 2.0, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (array: DoubleArray, offset: Int, length: Int) -> Double = DoubleArray::median, keepLast: Boolean = true, histogram: Histogram? = null): DoubleArray {
    val array = copyOfRange(offset, offset+length)
    val clippedLength = array.sigmaClipInplace(kappa, iterations, 0, length, standardDeviationType, center, keepLast, histogram)
    return array.copyOfRange(0, clippedLength)
}

fun FloatArray.sigmaClip(low: Float, high: Float, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (array: FloatArray, offset: Int, length: Int) -> Float = FloatArray::median, keepLast: Boolean = true, histogram: Histogram? = null): FloatArray {
    val array = copyOfRange(offset, offset+length)
    val clippedLength = array.sigmaClipInplace(low, high, 0, length, keepLast)
    return array.copyOfRange(0, clippedLength)
}

fun FloatArray.sigmaClipInplace(kappa: Float = 2f, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (array: FloatArray, offset: Int, length: Int) -> Float = FloatArray::median, keepLast: Boolean = true, histogram: Histogram? = null): Int {
    var currentLength = length

    for (i in 0 until iterations) {
        val sigma = stddev(standardDeviationType, offset, currentLength)
        val mid = center(this, offset, currentLength)

        val low = mid - kappa * sigma
        val high = mid + kappa * sigma

        currentLength = sigmaClipInplace(low, high, offset, currentLength, keepLast)
    }
    histogram?.add(currentLength)

    return currentLength
}

fun DoubleArray.sigmaClipInplace(kappa: Double = 2.0, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (array: DoubleArray, offset: Int, length: Int) -> Double = DoubleArray::median, keepLast: Boolean = true, histogram: Histogram? = null): Int {
    var currentLength = length

    for (i in 0 until iterations) {
        val sigma = stddev(standardDeviationType, offset, currentLength)
        val mid = center(this, offset, currentLength)

        val low = mid - kappa * sigma
        val high = mid + kappa * sigma

        currentLength = sigmaClipInplace(low, high, offset, currentLength, keepLast)
    }
    histogram?.add(currentLength)

    return currentLength
}

fun FloatArray.sigmaClipInplace(low: Float, high: Float, offset: Int = 0, length: Int = size-offset, keepLast: Boolean = true): Int {
    var targetLength = 0
    for (source in offset until (offset+length)) {
        if (this[source] in low..high) {
            this[offset + targetLength++] = this[source]
        }
    }

    if (keepLast && length > 0 && targetLength == 0) {
        val center = low + (high - low) / 2
        var best = this[offset]
        for (source in offset+1 until (offset + length)) {
            if (abs(center - this[source]) < abs(center - best)) {
                best = this[source]
            }
        }
        this[offset] = best
        targetLength = 1
    }

    return targetLength
}

fun DoubleArray.sigmaClipInplace(low: Double, high: Double, offset: Int = 0, length: Int = size - offset, keepLast: Boolean = true): Int {
    var targetLength = 0
    for (source in offset until (offset+length)) {
        if (this[source] in low..high) {
            this[offset + targetLength++] = this[source]
        }
    }

    if (keepLast && length > 0 && targetLength == 0) {
        val center = low + (high - low) / 2
        var best = this[offset]
        for (source in offset+1 until (offset + length)) {
            if (abs(center - this[source]) < abs(center - best)) {
                best = this[source]
            }
        }
        this[offset] = best
        targetLength = 1
    }

    return targetLength
}

fun FloatArray.huberWinsorizeInplace(offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population): FloatArray {
    val huberEpsilon = 0.0005
    val huberKappa = 1.5f
    val huberSigmaFactor = 1.345f
    var median = medianInplace(offset, length)
    var sigma = stddev(standardDeviationType, offset, length)
    do {
        val low = median - sigma * huberKappa
        val high = median + sigma * huberKappa
        winsorizeInplace(low, high, offset, length)

        median = medianInplace(offset, length)
        val lastSigma = sigma
        sigma = huberSigmaFactor * stddev(standardDeviationType, offset, length)
        val change = abs(sigma - lastSigma) / lastSigma
    } while (change > huberEpsilon)

    return this
}

fun FloatArray.huberWinsorizedSigmaClipInplace(kappa: Float, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, histogram: Histogram? = null): Int {
    huberWinsorizeInplace(offset, length, standardDeviationType)
    return sigmaClipInplace(kappa, iterations, offset, length, standardDeviationType, histogram = histogram)
}

fun FloatArray.huberWinsorizedSigmaClip(kappa: Float, iterations: Int = 1, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, histogram: Histogram? = null): FloatArray {
    val array = copyOfRange(offset, offset+length)
    val clippedLength = array.huberWinsorizedSigmaClipInplace(kappa, iterations, 0, length, standardDeviationType, histogram)
    return array.copyOfRange(0, clippedLength)
}

fun FloatArray.sigmaWinsorize(kappa: Float, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population): FloatArray {
    val array = copyOfRange(offset, offset+length)

    return array.sigmaWinsorizeInplace(kappa, 0, length, standardDeviationType)
}

fun DoubleArray.sigmaWinsorize(kappa: Double, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population): DoubleArray {
    val array = copyOfRange(offset, offset+length)

    return array.sigmaWinsorizeInplace(kappa, 0, length, standardDeviationType)
}

fun FloatArray.sigmaWinsorizeInplace(kappa: Float, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population): FloatArray {
    val sigma = stddev(standardDeviationType, offset, length)

    val m = median(offset, length)

    val low = m - kappa * sigma
    val high = m + kappa * sigma

    return winsorizeInplace(low, high, offset, length)
}

fun DoubleArray.sigmaWinsorizeInplace(kappa: Double, offset: Int = 0, length: Int = size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population): DoubleArray {
    val sigma = stddev(standardDeviationType, offset, length)

    val m = median(offset, length)

    val low = m - kappa * sigma
    val high = m + kappa * sigma

    return winsorizeInplace(low, high, offset, length)
}

fun FloatArray.winsorizeInplace(lowThreshold: Float, highThreshold: Float, offset: Int = 0, length: Int = size-offset): FloatArray {
    for (i in offset until (offset+length)) {
        this[i] = clamp(this[i], lowThreshold, highThreshold)
    }
    return this
}

fun DoubleArray.winsorizeInplace(lowThreshold: Double, highThreshold: Double, offset: Int = 0, length: Int = size-offset): DoubleArray {
    for (i in offset until (offset+length)) {
        this[i] = clamp(this[i], lowThreshold, highThreshold)
    }
    return this
}

fun FloatArray.weightedAverage(weightFunction: (i: Int, value: Float) -> Float, offset: Int = 0, length: Int = size-offset): Float {
    val weights = FloatArray(length)
    var totalWeight = 0.0f
    for (i in 0 until length) {
        val weight = weightFunction.invoke(i, this[i+offset])
        weights[i] = weight
        totalWeight += weight
    }

    var result = 0.0f
    for (i in 0 until length) {
        result += this[i+offset] * weights[i] / totalWeight
    }
    return result
}

fun DoubleArray.weightedAverage(weightFunction: (i: Int, value: Double) -> Double, offset: Int = 0, length: Int = size-offset): Double {
    val weights = DoubleArray(length)
    var totalWeight = 0.0
    for (i in 0 until length) {
        val weight = weightFunction.invoke(i, this[i+offset])
        weights[i] = weight
        totalWeight += weight
    }

    var result = 0.0
    for (i in 0 until length) {
        result += this[i+offset] * weights[i] / totalWeight
    }
    return result
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
