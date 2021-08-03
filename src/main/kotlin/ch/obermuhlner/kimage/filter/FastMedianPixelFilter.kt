package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.matrix.Matrix

class FastMedianPixelFilter(private val radius: Int, private val medianChannel: Channel = Channel.Luminance) : Filter<Image> {

    override fun filter(source: Image): Image {
        val matrix = source[medianChannel]

        return fastMedianPixel(matrix, source, radius)
    }

    class Histogram(private val binCount: Int = 256) {
        private val bins = IntArray(binCount)
        private val sampleY = IntArray(binCount)
        private val sampleX = IntArray(binCount)
        private var n = 0

        fun clear() {
            for (i in bins.indices) {
                bins[i] = 0
            }
            n = 0
        }

        fun add(matrix: Matrix, xStart: Int, yStart: Int, width: Int, height: Int) {
            for (y in yStart until yStart+height) {
                for (x in xStart until xStart+width) {
                    add(matrix[x, y], x, y)
                }
            }
        }

        fun remove(matrix: Matrix, xStart: Int, yStart: Int, width: Int, height: Int) {
            for (y in yStart until yStart+height) {
                for (x in xStart until xStart+width) {
                    remove(matrix[x, y])
                }
            }
        }

        fun add(value: Double, x: Int, y: Int) {
            val index = clamp((value * binCount).toInt(), 0, binCount - 1)
            bins[index]++
            sampleX[index] = x
            sampleY[index] = y
            n++
        }

        fun remove(value: Double) {
            val index = clamp((value * binCount).toInt(), 0, binCount - 1)
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
                    return Pair(sampleX[i], sampleY[i])
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

            for (y in 0 until matrix.height) {
                val forward = y % 2 == 0
                val xRange = if (forward) 0 until matrix.width else matrix.width-1 downTo 0
                for (x in xRange) {
                    val medianPixelXY = histogram.estimateMedianPixelIndex()
                    image.getPixel(medianPixelXY.first, medianPixelXY.second, pixel)
                    target.setPixel(x, y, pixel)

                    if (forward) {
                        if (x < matrix.width - 1) {
                            // move right
                            histogram.remove(matrix, x-radius, y-radius, 1, kernelSize)
                            histogram.add(matrix, x+radius+1, y-radius, 1, kernelSize)
                        } else {
                            // move down
                            histogram.remove(matrix, x-radius, y-radius, kernelSize, 1)
                            histogram.add(matrix, x-radius, y+radius+1, kernelSize, 1)
                        }
                    } else {
                        if (x > 0) {
                            // move left
                            histogram.remove(matrix, x+radius, y-radius, 1, kernelSize)
                            histogram.add(matrix, x-radius-1, y-radius, 1, kernelSize)
                        } else {
                            // move down
                            histogram.remove(matrix, x-radius, y-radius, kernelSize, 1)
                            histogram.add(matrix, x-radius, y+radius+1, kernelSize, 1)
                        }
                    }
                }
            }
            return target
        }
    }
}