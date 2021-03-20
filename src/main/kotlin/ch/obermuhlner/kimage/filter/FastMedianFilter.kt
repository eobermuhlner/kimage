package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.max
import kotlin.math.min

class FastMedianFilter(private val radius: Int, recursive: Boolean = false) : MatrixImageFilter({ _, source -> fastMedianMatrix(source, radius, recursive) }) {

    class Histogram(private val binCount: Int = 256) {
        private val bins = IntArray(binCount)
        private var n = 0

        fun clear() {
            for (i in bins.indices) {
                bins[i] = 0
            }
            n = 0
        }

        fun add(matrix: Matrix, rowStart: Int, columnStart: Int, rows: Int, columns: Int) {
            for (row in rowStart until rows) {
                for (column in columnStart until columns) {
                    add(matrix[row, column])
                }
            }
        }

        fun remove(matrix: Matrix, rowStart: Int, columnStart: Int, rows: Int, columns: Int) {
            for (row in rowStart until rows) {
                for (column in columnStart until columns) {
                    remove(matrix[row, column])
                }
            }
        }

        fun add(value: Double) {
            val index = max(0, min(binCount - 1, (value * binCount).toInt()))
            bins[index]++
            n++
        }

        fun remove(value: Double) {
            val index = max(0, min(binCount - 1, (value * binCount).toInt()))
            bins[index]--
            n--
        }

        fun estimateMean(): Double {
            var sum = 0.0
            for (i in bins.indices) {
                sum += bins[i] * (i.toDouble() + 0.5) / (binCount - 1).toDouble()
            }
            return sum / n
        }

        fun estimateMedian(): Double {
            val nHalf = n / 2
            var cumulativeN = 0
            for (i in bins.indices) {
                if (cumulativeN + bins[i] >= nHalf) {
                    val lowerLimit = i.toDouble() / (binCount - 1).toDouble()
                    val width = 1.0 / (binCount - 1).toDouble()
                    return lowerLimit + (nHalf - cumulativeN) / bins[i].toDouble() * width
                }
                cumulativeN += bins[i]
            }
            return 0.0
        }
    }

    companion object {
        fun fastMedianMatrix(source: Matrix, radius: Int, recursive: Boolean = false): Matrix {
            val sourceCopy = if (recursive) source.copy() else source
            val target = source.create()
            val kernelSize = radius+radius+1

            val histogram = Histogram()

            //histogram.add(sourceCopy, -radius, -radius, kernelSize, kernelSize)

            for (row in 0 until source.rows) {
                for (column in 0 until source.columns) {
                    histogram.clear()
                    histogram.add(sourceCopy, row-radius, column-radius, row+radius, column+radius)
                    val medianValue = histogram.estimateMedian()
                    if (recursive) {
                        sourceCopy[row, column] = medianValue
                    }
                    target[row, column] = medianValue
                }
            }
            return target
        }
    }
}