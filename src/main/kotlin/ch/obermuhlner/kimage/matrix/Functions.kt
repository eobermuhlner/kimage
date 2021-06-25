package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.math.mix

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

enum class Scaling {
    Nearest,
    Bilinear
}

fun Matrix.scaleBy(scaleRows: Double, scaleColumns: Double, scaling: Scaling = Scaling.Bilinear): Matrix {
    val newRows = (rows * scaleRows).toInt()
    val newColumns = (columns * scaleColumns).toInt()

    return scaleTo(newRows, newColumns, scaling)
}

fun Matrix.scaleTo(newRows: Int, newColumns: Int, scaling: Scaling = Scaling.Bilinear): Matrix {
    return when (scaling) {
        Scaling.Nearest -> scaleNearestTo(newRows, newColumns)
        Scaling.Bilinear -> scaleBilinearTo(newRows, newColumns)
    }
}

private fun Matrix.scaleNearestTo(newRows: Int, newColumns: Int): Matrix {
    val m = create(newRows, newColumns)
    for (newRow in 0 until newRows) {
        for (newColumn in 0 until newColumns) {
            val oldRow = (newRow.toDouble() / newRows * (rows - 1)).toInt()
            val oldColumn = (newColumn.toDouble() / newColumns * (columns - 1)).toInt()

            val newValue = this[oldRow, oldColumn]
            m[newRow, newColumn] = newValue
        }
    }

    return m
}

private fun Matrix.scaleBilinearTo(newRows: Int, newColumns: Int): Matrix {
    val m = create(newRows, newColumns)
    for (newRow in 0 until newRows) {
        for (newColumn in 0 until newColumns) {
            val oldRow = newRow.toDouble() / newRows * (rows - 1)
            val oldColumn = newColumn.toDouble() / newColumns * (columns - 1)
            val oldRowInt = oldRow.toInt()
            val oldColumnInt = oldColumn.toInt()

            val v00 = this[oldRowInt, oldColumnInt]
            val v01 = this[oldRowInt, oldColumnInt + 1]
            val v10 = this[oldRowInt + 1, oldColumnInt]
            val v11 = this[oldRowInt + 1, oldColumnInt + 1]

            val newValue = mix(v00, v01, v10, v11, oldRow - oldRowInt, oldColumn - oldColumnInt)

            m[newRow, newColumn] = newValue
        }
    }

    return m
}
