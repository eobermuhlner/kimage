package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.Scaling
import ch.obermuhlner.kimage.math.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun Matrix.getPixel(x: Int, y: Int) = this[y, x]

fun Matrix.setPixel(x: Int, y: Int, value: Double) {
    this[y, x] = value
}

fun Matrix.contentToString(multiline: Boolean = false): String {
    val str = StringBuilder()

    str.append("[")
    for (row in 0 until rows) {
        if (row != 0) {
            str.append(" ")
        }
        str.append("[")
        for (column in 0 until columns) {
            if (column != 0) {
                str.append(" ,")
            }
            str.append(this[row, column])
        }
        str.append("]")
        if (multiline && row != rows - 1) {
            str.appendLine()
        }
    }
    str.append("]")
    if (multiline) {
        str.appendLine()
    }

    return str.toString()
}

fun Matrix.contentEquals(other: Matrix, epsilon: Double = 1E-10): Boolean {
    if (rows != other.rows || columns != other.columns) {
        return false
    }

    for (row in 0 until rows) {
        for (column in 0 until columns) {
            if (abs(this[row, column] - other[row, column]) > epsilon) {
                return false
            }
        }
    }

    return true
}

fun max(m1: Matrix, m2: Matrix): Matrix {
    val m = m1.create()
    for (row in 0 until m1.rows) {
        for (column in 0 until m1.columns) {
            m[row, column] = kotlin.math.max(m1[row, column], m2[row, column])
        }
    }
    return m
}

fun Matrix.stretchClassic(min: Double, max: Double, func: (value: Double) -> Double = { it }): Matrix {
    val denom = func(max - min)
    return this.copy().onEach { value ->
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
            val oldRow = (newRow.toDouble() / newRows * rows).toInt()
            val oldColumn = (newColumn.toDouble() / newColumns * columns).toInt()

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

fun Matrix.interpolate(fixPoints: List<Pair<Int, Int>>, valueFunc: (Pair<Int, Int>) -> Double = { valueAt(this, it.first, it.second) }, power: Double = estimatePowerForInterpolation(fixPoints.size)): Matrix {
    val fixValues = fixPoints.map { valueFunc(it) }
    return interpolate(fixPoints, fixValues, power)
}

fun Matrix.interpolate(fixPoints: List<Pair<Int, Int>>, fixValues: List<Double>, power: Double = estimatePowerForInterpolation(fixPoints.size)): Matrix {
    val m = create()

    for (row in 0 until rows) {
        for (column in 0 until columns) {
            m[row, column] = interpolate(row, column, fixPoints, fixValues, power)
        }
    }

    return m
}

fun valueAt(matrix: Matrix, row: Int, column: Int): Double {
    return matrix[row, column]
}

fun medianAround(matrix: Matrix, row: Int, column: Int, radius: Int = 10): Double {
    return matrix.croppedMatrix(row - radius, column - radius, radius+radius+1, radius+radius+1).median()
}

private fun interpolate(row: Int, column: Int, fixPoints: List<Pair<Int, Int>>, fixValues: List<Double>, power: Double = estimatePowerForInterpolation(fixPoints.size)): Double {
    require(fixPoints.size == fixValues.size)

    val distances = DoubleArray(fixPoints.size)
    var totalDistance = 0.0
    for (i in fixPoints.indices) {
        val fixRow = fixPoints[i].first
        val fixColumn = fixPoints[i].second

        val dRow = (row-fixRow).toDouble()
        val dColumn = (column-fixColumn).toDouble()
        val distance = sqrt(dRow*dRow + dColumn*dColumn)
        distances[i] = distance
        totalDistance += distance
    }

    val factors = DoubleArray(fixPoints.size)
    var totalFactor = 0.0
    for (i in fixPoints.indices) {
        var factor = 1.0 - distances[i] / totalDistance
        factor = factor.pow(power)
        factors[i] = factor
        totalFactor += factor
    }

    var mixedValue = 0.0
    for (i in fixPoints.indices) {
        val fixValue = fixValues[i]
        val factor = factors[i] / totalFactor
        mixedValue += fixValue * factor
    }

    return mixedValue
}

fun estimatePowerForInterpolation(n: Int): Double = n.toDouble().pow(1.6)


