package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix
import java.util.*

class MedianFilter(private val radius: Int, private val shape: Shape = Shape.Square) : MatrixImageFilter({ _, source -> medianMatrix(source, radius, shape) }) {

    companion object {
        fun medianMatrix(source: Matrix, radius: Int, shape: Shape): Matrix {
            val target = source.create()
            val kernelSize = radius+radius+1
            val n = kernelSize * kernelSize
            val values = DoubleArray(n)

            for (row in 0 until source.rows) {
                for (column in 0 until source.columns) {
                    target[row, column] = median(source, row, column, radius, shape, values)
                }
            }
            return target
        }

        private fun median(matrix: Matrix, row: Int, column: Int, radius: Int, shape: Shape, values: DoubleArray): Double {
            var n = 0

            when (shape) {
                Shape.Cross -> {
                    for (dr in -radius..radius) {
                        values[n++] = matrix[row + dr, column]
                    }
                    for (dc in -radius until 0) {
                        values[n++] = matrix[row, column + dc]
                    }
                    for (dc in 1 .. radius) {
                        values[n++] = matrix[row, column + dc]
                    }
                }
                Shape.DiagonalCross -> {
                    values[n++] = matrix[row, column]
                    for (r in 1..radius) {
                        values[n++] = matrix[row + r, column + r]
                        values[n++] = matrix[row + r, column - r]
                        values[n++] = matrix[row - r, column + r]
                        values[n++] = matrix[row - r, column - r]
                    }
                }
                Shape.Star -> {
                    values[n++] = matrix[row, column]
                    for (r in 1..radius) {
                        values[n++] = matrix[row + r, column]
                        values[n++] = matrix[row - r, column]
                        values[n++] = matrix[row, column + r]
                        values[n++] = matrix[row, column - r]

                        values[n++] = matrix[row + r, column + r]
                        values[n++] = matrix[row + r, column - r]
                        values[n++] = matrix[row - r, column + r]
                        values[n++] = matrix[row - r, column - r]
                    }
                }
                else -> {
                    val horizontalRadius = shape.horizontalRadius(radius)
                    val verticalRadius = shape.verticalRadius(radius)
                    for (dr in -verticalRadius .. verticalRadius) {
                        for (dc in -horizontalRadius .. horizontalRadius) {
                            if (shape.isInside(dr, dc, radius)) {
                                values[n++] = matrix[row + dr, column + dc]
                            }
                        }
                    }
                }
            }

            Arrays.sort(values)

            return if (n % 2 == 0) {
                (values[n/2] + values[n/2+1]) / 2
            } else {
                values[n/2]
            }
        }
    }
}