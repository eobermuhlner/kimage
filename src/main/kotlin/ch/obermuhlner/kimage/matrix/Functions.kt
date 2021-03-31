package ch.obermuhlner.kimage.matrix

fun Matrix.stretchClassic(min: Double, max: Double, func: (Double) -> Double = { it }): Matrix {
    val denom = func(max - min)
    return this.onEach { value ->
        when {
            value < min -> 0.0
            value > max -> 1.0
            else -> func(value - min) / denom
        }
    }
}

fun Matrix.averageError(other: Matrix): Double {
    var sum = 0.0
    for (index in 0 until size) {
        val delta = this[index] - other[index]
        sum += delta * delta
    }
    return sum / size
}

