package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix
import java.util.*

class MedianFilter(private val radius: Int, private val shape: Shape = Shape.Square, recursive: Boolean = false) : MatrixImageFilter({ _, source -> medianMatrix(source, radius, shape, recursive) }) {

    companion object {
        fun medianMatrix(source: Matrix, radius: Int, shape: Shape, recursive: Boolean = false): Matrix {
            val sourceCopy = if (recursive) source.copy() else source
            val target = source.create()
            val kernelSize = radius+radius+1
            val n = kernelSize * kernelSize
            val values = DoubleArray(n)

            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    val medianValue = median(sourceCopy, x, y, radius, shape, values)
                    if (recursive) {
                        sourceCopy[x, y] = medianValue
                    }
                    target[x, y] = medianValue
                }
            }
            return target
        }

        private fun median(matrix: Matrix, x: Int, y: Int, radius: Int, shape: Shape, values: DoubleArray): Double {
            var n = 0

            when (shape) {
                Shape.Cross -> {
                    for (dr in -radius..radius) {
                        values[n++] = matrix[x, y + dr]
                    }
                    for (dc in -radius until 0) {
                        values[n++] = matrix[x + dc, y]
                    }
                    for (dc in 1 .. radius) {
                        values[n++] = matrix[x + dc, y]
                    }
                }
                Shape.DiagonalCross -> {
                    values[n++] = matrix[x, y]
                    for (r in 1..radius) {
                        values[n++] = matrix[x + r, y + r]
                        values[n++] = matrix[x - r, y + r]
                        values[n++] = matrix[x + r, y - r]
                        values[n++] = matrix[x - r, y - r]
                    }
                }
                Shape.Star -> {
                    values[n++] = matrix[x, y]
                    for (r in 1..radius) {
                        values[n++] = matrix[x, y + r]
                        values[n++] = matrix[x, y - r]
                        values[n++] = matrix[x + r, y]
                        values[n++] = matrix[x - r, y]

                        values[n++] = matrix[x + r, y + r]
                        values[n++] = matrix[x - r, y + r]
                        values[n++] = matrix[x + r, y - r]
                        values[n++] = matrix[x - r, y - r]
                    }
                }
                else -> {
                    val horizontalRadius = shape.horizontalRadius(radius)
                    val verticalRadius = shape.verticalRadius(radius)
                    for (dr in -verticalRadius .. verticalRadius) {
                        for (dc in -horizontalRadius .. horizontalRadius) {
                            if (shape.isInside(dr, dc, radius)) {
                                values[n++] = matrix[x + dc, y + dr]
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