package ch.obermuhlner.kimage.math

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

fun FloatArray.median(): Float {
    return toMutableList().median()
}

fun MutableList<Float>.median(): Float {
    if (size == 0) {
        return Float.NaN
    }
    this.sort()

    val half = size / 2
    if (size % 2 == 0) {
        return (this[half-1] + this[half]) / 2
    } else {
        return this[half]
    }
}

fun DoubleArray.median(): Double {
    return toMutableList().median()
}

fun MutableList<Double>.median(): Double {
    if (size == 0) {
        return Double.NaN
    }

    sort()

    if (size % 2 == 0) {
        return (this[size/2] + this[size+1]) / 2
    } else {
        return this[size/2]
    }
}

fun FloatArray.stddev(): Float {
    return toList().stddev()
}

fun List<Float>.stddev(): Float {
    val avg = average().toFloat()
    var sum = 0.0f
    for (value in this) {
        val delta = value - avg
        sum += delta * delta
    }
    return if (size == 0) {
        Float.NaN
    } else {
        sum / size
    }
}

fun FloatArray.medianSigmaClip(alpha: Float = 2f, iterations: Int = 1): FloatArray {
    var result = this.toMutableList()

    for (i in 0 until iterations) {
        val sigma = result.stddev()
        val m = result.median()

        val low = m - alpha * sigma
        val high = m + alpha * sigma

        result = this.filter { it in low..high } .toMutableList()
    }

    return result.toFloatArray()
}
