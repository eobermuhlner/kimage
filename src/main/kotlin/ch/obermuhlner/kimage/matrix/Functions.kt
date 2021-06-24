package ch.obermuhlner.kimage.matrix

fun max(m1: Matrix, m2: Matrix): Matrix {
    val m = m1.create()
    for (row in 0 until m1.rows) {
        for (column in 0 until m1.columns) {
            m[row, column] = kotlin.math.max(m1[row, column], m2[row, column])
        }
    }
    return m
}

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

