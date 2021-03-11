package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix
import java.util.*

class AverageFilter(private val radius: Int, private val shape: Shape = Shape.Square) : MatrixImageFilter({ _, source -> averageMatrix(source, radius, shape) }) {

    companion object {
        fun averageMatrix(source: Matrix, radius: Int, shape: Shape): Matrix {
            val target = source.create()
            val kernelSize = radius+radius+1
            val n = kernelSize * kernelSize
            val values = DoubleArray(n)

            for (row in 0 until source.rows) {
                for (column in 0 until source.columns) {
                    target[row, column] = average(source, row, column, radius, shape, values)
                }
            }
            return target
        }

        private fun average(matrix: Matrix, row: Int, column: Int, radius: Int, shape: Shape, values: DoubleArray): Double {
            var n = 0

            var sum = 0.0
            when (shape) {
                Shape.Cross -> {
                    for (dr in -radius..radius) {
                        sum += matrix[row + dr, column]
                        n++
                    }
                    for (dc in -radius until 0) {
                        sum += matrix[row, column + dc]
                        n++
                    }
                    for (dc in 1 .. radius) {
                        sum += matrix[row, column + dc]
                        n++
                    }
                }
                Shape.DiagonalCross -> {
                    values[n++] = matrix[row, column]
                    for (r in 1..radius) {
                        sum += matrix[row + r, column + r]
                        sum += matrix[row + r, column - r]
                        sum += matrix[row - r, column + r]
                        sum += matrix[row - r, column - r]
                        n += 4
                    }
                }
                Shape.Star -> {
                    values[n++] = matrix[row, column]
                    for (r in 1..radius) {
                        sum += matrix[row + r, column]
                        sum += matrix[row - r, column]
                        sum += matrix[row, column + r]
                        sum += matrix[row, column - r]

                        sum += matrix[row + r, column + r]
                        sum += matrix[row + r, column - r]
                        sum += matrix[row - r, column + r]
                        sum += matrix[row - r, column - r]
                        n += 8
                    }
                }
                else -> {
                    val horizontalRadius = shape.horizontalRadius(radius)
                    val verticalRadius = shape.verticalRadius(radius)
                    for (dr in -verticalRadius .. verticalRadius) {
                        for (dc in -horizontalRadius .. horizontalRadius) {
                            if (shape.isInside(dr, dc, radius)) {
                                sum += matrix[row + dr, column + dc]
                                n++
                            }
                        }
                    }
                }
            }

            return sum / n
        }
    }
}