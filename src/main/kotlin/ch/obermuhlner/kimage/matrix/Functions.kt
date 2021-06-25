package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.Scaling
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.math.mixBilinear
import ch.obermuhlner.kimage.math.mixCubicHermite

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

fun Matrix.scaleBy(scaleRows: Double, scaleColumns: Double, scaling: Scaling = Scaling.Bicubic): Matrix {
    val newRows = (rows * scaleRows).toInt()
    val newColumns = (columns * scaleColumns).toInt()

    return scaleTo(newRows, newColumns, scaling)
}

fun Matrix.scaleTo(newRows: Int, newColumns: Int, scaling: Scaling = Scaling.Bicubic): Matrix {
    return when (scaling) {
        Scaling.Nearest -> scaleNearestTo(newRows, newColumns)
        Scaling.Bilinear -> scaleBilinearTo(newRows, newColumns)
        Scaling.Bicubic -> scaleBicubicTo(newRows, newColumns)
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
            val oldRow = newRow.toDouble() / newRows * (rows - 1) - 0.5
            val oldColumn = newColumn.toDouble() / newColumns * (columns - 1) - 0.5
            val oldRowInt = oldRow.toInt()
            val oldColumnInt = oldColumn.toInt()
            val oldRowFract = oldRow - oldRowInt
            val oldColumnFract = oldColumn - oldColumnInt

            val v00 = this[oldRowInt, oldColumnInt]
            val v01 = this[oldRowInt, oldColumnInt + 1]
            val v10 = this[oldRowInt + 1, oldColumnInt]
            val v11 = this[oldRowInt + 1, oldColumnInt + 1]

            val newValue = mixBilinear(v00, v01, v10, v11, oldRowFract, oldColumnFract)

            m[newRow, newColumn] = newValue
        }
    }

    return m
}

private fun Matrix.scaleBicubicTo(newRows: Int, newColumns: Int): Matrix {
    val m = create(newRows, newColumns)
    for (newRow in 0 until newRows) {
        for (newColumn in 0 until newColumns) {
            val oldRow = newRow.toDouble() / newRows * (rows - 1) - 0.5
            val oldColumn = newColumn.toDouble() / newColumns * (columns - 1) - 0.5
            val oldRowInt = oldRow.toInt()
            val oldColumnInt = oldColumn.toInt()
            val oldRowFract = oldRow - oldRowInt
            val oldColumnFract = oldColumn - oldColumnInt

            val v00 = this[oldRowInt - 1, oldColumnInt - 1]
            val v10 = this[oldRowInt + 0, oldColumnInt - 1]
            val v20 = this[oldRowInt + 1, oldColumnInt - 1]
            val v30 = this[oldRowInt + 2, oldColumnInt - 1]

            val v01 = this[oldRowInt - 1, oldColumnInt + 0]
            val v11 = this[oldRowInt + 0, oldColumnInt + 0]
            val v21 = this[oldRowInt + 1, oldColumnInt + 0]
            val v31 = this[oldRowInt + 2, oldColumnInt + 0]

            val v02 = this[oldRowInt - 1, oldColumnInt + 1]
            val v12 = this[oldRowInt + 0, oldColumnInt + 1]
            val v22 = this[oldRowInt + 1, oldColumnInt + 1]
            val v32 = this[oldRowInt + 2, oldColumnInt + 1]

            val v03 = this[oldRowInt - 1, oldColumnInt + 2]
            val v13 = this[oldRowInt + 0, oldColumnInt + 2]
            val v23 = this[oldRowInt + 1, oldColumnInt + 2]
            val v33 = this[oldRowInt + 2, oldColumnInt + 2]

            val col0 = mixCubicHermite(v00, v10, v20, v30, oldRowFract)
            val col1 = mixCubicHermite(v01, v11, v21, v31, oldRowFract)
            val col2 = mixCubicHermite(v02, v12, v22, v32, oldRowFract)
            val col3 = mixCubicHermite(v03, v13, v23, v33, oldRowFract)
            val newValue = mixCubicHermite(col0, col1, col2, col3, oldColumnFract)

            m[newRow, newColumn] = clamp(newValue, 0.0, 1.0)
        }
    }

    return m
}
