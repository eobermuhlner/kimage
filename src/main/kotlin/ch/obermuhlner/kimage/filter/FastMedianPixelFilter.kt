package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.max
import kotlin.math.min

class FastMedianPixelFilter(private val radius: Int, private val medianChannel: Channel = Channel.Luminance) : Filter<Image> {

    override fun filter(source: Image): Image {
        val matrix = source[medianChannel]

        return fastMedianPixel(matrix, source, radius)
    }

    class Histogram(private val binCount: Int = 256) {
        private val bins = IntArray(binCount)
        private val sampleRow = IntArray(binCount)
        private val sampleColumn = IntArray(binCount)
        private var n = 0

        fun clear() {
            for (i in bins.indices) {
                bins[i] = 0
            }
            n = 0
        }

        fun add(matrix: Matrix, rowStart: Int, columnStart: Int, rows: Int, columns: Int) {
            for (row in rowStart until rowStart+rows) {
                for (column in columnStart until columnStart+columns) {
                    add(matrix[row, column], row, column)
                }
            }
        }

        fun remove(matrix: Matrix, rowStart: Int, columnStart: Int, rows: Int, columns: Int) {
            for (row in rowStart until rowStart+rows) {
                for (column in columnStart until columnStart+columns) {
                    remove(matrix[row, column])
                }
            }
        }

        fun add(value: Double, row: Int, column: Int) {
            val index = max(0, min(binCount - 1, (value * binCount).toInt()))
            bins[index]++
            sampleRow[index] = row
            sampleColumn[index] = column
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

        fun estimateMedianPixelIndex(): Pair<Int, Int> {
            val nHalf = n / 2
            var cumulativeN = 0
            for (i in bins.indices) {
                if (cumulativeN + bins[i] >= nHalf) {
                    return Pair(sampleRow[i], sampleColumn[i])
                }
                cumulativeN += bins[i]
            }
            return Pair(0, 0)
        }
    }

    companion object {
        fun fastMedianPixel(matrix: Matrix, image: Image, radius: Int): Image {
            val target = MatrixImage(image.width, image.height, image.channels)
            val kernelSize = radius+radius+1

            val histogram = Histogram()
            val pixel = DoubleArray(image.channels.size)

            histogram.add(matrix, -radius, -radius, kernelSize, kernelSize)

            for (row in 0 until matrix.rows) {
                val forward = row % 2 == 0
                val columnRange = if (forward) 0 until matrix.columns else matrix.columns-1 downTo 0
                for (column in columnRange) {
                    val medianPixelRowColumn = histogram.estimateMedianPixelIndex()
                    image.getPixel(medianPixelRowColumn.second, medianPixelRowColumn.first, pixel)
                    target.setPixel(column, row, pixel)

                    if (forward) {
                        if (column < matrix.columns - 1) {
                            // move right
                            histogram.remove(matrix, row-radius, column-radius, kernelSize, 1)
                            histogram.add(matrix, row-radius, column+radius+1, kernelSize, 1)
                        } else {
                            // move down
                            histogram.remove(matrix, row-radius, column-radius, 1, kernelSize)
                            histogram.add(matrix, row+radius+1, column-radius, 1, kernelSize)
                        }
                    } else {
                        if (column > 0) {
                            // move left
                            histogram.remove(matrix, row-radius, column+radius, kernelSize, 1)
                            histogram.add(matrix, row-radius, column-radius-1, kernelSize, 1)
                        } else {
                            // move down
                            histogram.remove(matrix, row-radius, column-radius, 1, kernelSize)
                            histogram.add(matrix, row+radius+1, column-radius, 1, kernelSize)
                        }
                    }
                }
            }
            return target
        }
    }
}