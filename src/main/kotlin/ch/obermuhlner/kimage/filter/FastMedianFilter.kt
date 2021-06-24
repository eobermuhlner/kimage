package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.math.Histogram
import ch.obermuhlner.kimage.matrix.Matrix

class FastMedianFilter(private val radius: Int, recursive: Boolean = false) : MatrixImageFilter({ _, source -> fastMedianMatrix(source, radius, recursive) }) {

    companion object {
        fun fastMedianMatrix(source: Matrix, radius: Int, recursive: Boolean = false): Matrix {
            val sourceCopy = if (recursive) source.copy() else source
            val target = source.create()
            val kernelSize = radius+radius+1

            val histogram = Histogram()

            histogram.add(sourceCopy, -radius, -radius, kernelSize, kernelSize)

            for (row in 0 until source.rows) {
                val forward = row % 2 == 0
                val columnRange = if (forward) 0 until source.columns else source.columns-1 downTo 0
                for (column in columnRange) {
                    val medianValue = histogram.estimateMedian()
                    if (recursive) {
                        histogram.remove(sourceCopy[row, column])
                        sourceCopy[row, column] = medianValue
                        histogram.add(medianValue)
                    }
                    target[row, column] = medianValue
                    if (forward) {
                        if (column < source.columns - 1) {
                            // move right
                            histogram.remove(sourceCopy, row-radius, column-radius, kernelSize, 1)
                            histogram.add(sourceCopy, row-radius, column+radius+1, kernelSize, 1)
                        } else {
                            // move down
                            histogram.remove(sourceCopy, row-radius, column-radius, 1, kernelSize)
                            histogram.add(sourceCopy, row+radius+1, column-radius, 1, kernelSize)
                        }
                    } else {
                        if (column > 0) {
                            // move left
                            histogram.remove(sourceCopy, row-radius, column+radius, kernelSize, 1)
                            histogram.add(sourceCopy, row-radius, column-radius-1, kernelSize, 1)
                        } else {
                            // move down
                            histogram.remove(sourceCopy, row-radius, column-radius, 1, kernelSize)
                            histogram.add(sourceCopy, row+radius+1, column-radius, 1, kernelSize)
                        }
                    }
                }
            }
            return target
        }
    }
}