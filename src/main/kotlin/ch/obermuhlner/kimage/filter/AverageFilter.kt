package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix

class AverageFilter(private val radius: Int, private val shape: Shape = Shape.Square) : MatrixImageFilter({ _, source -> averageMatrix(source, radius, shape) }) {

    companion object {
        fun averageMatrix(source: Matrix, radius: Int, shape: Shape): Matrix {
            val target = source.create()
            val kernelSize = radius+radius+1
            val n = kernelSize * kernelSize
            val values = DoubleArray(n)

            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    target[x, y] = average(source, x, y, radius, shape, values)
                }
            }
            return target
        }

        private fun average(matrix: Matrix, x: Int, y: Int, radius: Int, shape: Shape, values: DoubleArray): Double {
            var n = 0

            var sum = 0.0
            when (shape) {
                Shape.Cross -> {
                    for (dr in -radius..radius) {
                        sum += matrix[x, y + dr]
                        n++
                    }
                    for (dc in -radius until 0) {
                        sum += matrix[x + dc, y]
                        n++
                    }
                    for (dc in 1 .. radius) {
                        sum += matrix[x + dc, y]
                        n++
                    }
                }
                Shape.DiagonalCross -> {
                    values[n++] = matrix[x, y]
                    for (r in 1..radius) {
                        sum += matrix[x + r, y + r]
                        sum += matrix[x - r, y + r]
                        sum += matrix[x + r, y - r]
                        sum += matrix[x - r, y - r]
                        n += 4
                    }
                }
                Shape.Star -> {
                    values[n++] = matrix[x, y]
                    for (r in 1..radius) {
                        sum += matrix[x, y + r]
                        sum += matrix[x, y - r]
                        sum += matrix[x + r, y]
                        sum += matrix[x - r, y]

                        sum += matrix[x + r, y + r]
                        sum += matrix[x - r, y + r]
                        sum += matrix[x + r, y - r]
                        sum += matrix[x - r, y - r]
                        n += 8
                    }
                }
                else -> {
                    val horizontalRadius = shape.horizontalRadius(radius)
                    val verticalRadius = shape.verticalRadius(radius)
                    for (dr in -verticalRadius .. verticalRadius) {
                        for (dc in -horizontalRadius .. horizontalRadius) {
                            if (shape.isInside(dr, dc, radius)) {
                                sum += matrix[x + dc, y + dr]
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