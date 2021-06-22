package ch.obermuhlner.kimage.math

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

fun FloatArray.sum(offset: Int = 0, length: Int = this.size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }

    var sum = 0.0f
    for (i in offset until (offset+length)) {
        sum += this[i]
    }

    return sum
}

fun FloatArray.average(offset: Int = 0, length: Int = this.size-offset): Float {
    return sum(offset, length) / length
}

enum class StandardDeviation { Population, Sample }
fun FloatArray.stddev(type: StandardDeviation = StandardDeviation.Population, offset: Int = 0, length: Int = this.size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }
    if (length == 1) {
        return 0f
    }

    val avg = average(offset, length)
    var sum = 0f
    for (i in offset until (offset+length)) {
        val delta = this[i] - avg
        sum += delta * delta
    }

    val denom = when (type) {
        StandardDeviation.Population -> length
        StandardDeviation.Sample -> length - 1
    }
    return sqrt(sum / denom)
}

fun FloatArray.medianInplace(offset: Int = 0, length: Int = this.size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }

    this.sort(offset, offset+length)

    val half = offset + length / 2
    if (length % 2 == 0) {
        return (this[half-1] + this[half]) / 2
    } else {
        return this[half]
    }
}

fun FloatArray.median(offset: Int = 0, length: Int = this.size-offset): Float {
    if (length == 0) {
        return Float.NaN
    }

    val array = this.copyOfRange(offset, offset+length)
    return array.medianInplace()
}

fun FloatArray.sigmaClipInplace(kappa: Float = 2f, iterations: Int = 1, offset: Int = 0, length: Int = this.size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (FloatArray, Int, Int) -> Float = FloatArray::median): FloatArray {
    var currentLength = length

    for (i in 0 until iterations) {
        val sigma = this.stddev(standardDeviationType, offset, currentLength)
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

    return this.copyOfRange(offset, offset+currentLength)
}

fun FloatArray.sigmaClip(kappa: Float = 2f, iterations: Int = 1, offset: Int = 0, length: Int = this.size-offset, standardDeviationType: StandardDeviation = StandardDeviation.Population, center: (FloatArray, Int, Int) -> Float = FloatArray::median): FloatArray {
    val array = this.copyOfRange(offset, offset+length)

    return array.sigmaClipInplace(kappa, iterations, 0, length, standardDeviationType, center)
}

