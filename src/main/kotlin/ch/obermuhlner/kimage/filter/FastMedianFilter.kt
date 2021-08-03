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

            for (y in 0 until source.height) {
                val forward = y % 2 == 0
                val xRange = if (forward) 0 until source.width else source.width-1 downTo 0
                for (x in xRange) {
                    val medianValue = histogram.estimateMedian()
                    if (recursive) {
                        histogram.remove(sourceCopy[x, y])
                        sourceCopy[x, y] = medianValue
                        histogram.add(medianValue)
                    }
                    target[x, y] = medianValue
                    if (forward) {
                        if (x < source.width - 1) {
                            // move right
                            histogram.remove(sourceCopy, x-radius, y-radius, 1, kernelSize)
                            histogram.add(sourceCopy, x+radius+1, y-radius, 1, kernelSize)
                        } else {
                            // move down
                            histogram.remove(sourceCopy, x-radius, y-radius, kernelSize, 1)
                            histogram.add(sourceCopy, x-radius, y+radius+1, kernelSize, 1)
                        }
                    } else {
                        if (x > 0) {
                            // move left
                            histogram.remove(sourceCopy, x+radius, y-radius, 1, kernelSize)
                            histogram.add(sourceCopy, x-radius-1, y-radius, 1, kernelSize)
                        } else {
                            // move down
                            histogram.remove(sourceCopy, x-radius, y-radius, kernelSize, 1)
                            histogram.add(sourceCopy, x-radius, y+radius+1, kernelSize, 1)
                        }
                    }
                }
            }
            return target
        }
    }
}