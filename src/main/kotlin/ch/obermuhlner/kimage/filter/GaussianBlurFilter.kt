package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sqrt

class GaussianBlurFilter(private val radius: Int) : MatrixImageFilter({_, source -> blur(source, radius) }) {

    private class SwapMatrix(var source: Matrix, var target: Matrix) {
        fun swap() {
            val tmp: Matrix = source
            source = target
            target = tmp
        }
    }

    companion object {
        fun blur(source: Matrix, radius: Int): Matrix {
            val temp = SwapMatrix(source.create(), source.create())

            temp.source.set(source)

            val boxSizes = boxSizesForGauss(radius.toDouble(), 3)
            for (boxSize in boxSizes) {
                val boxRadius = (ceil((boxSize - 1) / 2) + 0.5).toInt()
                boxBlur(temp.source, temp.target, boxRadius)
                temp.swap()
            }

            return temp.source
        }

        private fun boxBlur(source: Matrix, target: Matrix, boxRadius: Int) {
            val tempMatrix = source.create()
            boxBlurHorizontal(source, tempMatrix, boxRadius)
            boxBlurVertical(tempMatrix, target, boxRadius)
        }

        private fun boxBlurHorizontal(source: Matrix, target: Matrix, boxRadius: Int) {
            val kernelSize = boxRadius + boxRadius + 1

            for (y in 0 until source.height) {
                val first = source[0, y]
                val last = source[source.width - 1, y]

                var leftX = 0
                var rightX = boxRadius
                var targetX = 0

                var sum = first * (boxRadius + 1)
                for (x in 0 until boxRadius) {
                    sum += source[x, y]
                }
                for (x in 0..boxRadius) {
                    sum += source[rightX++, y] - first
                    target[targetX++, y] = sum / kernelSize
                }
                for (x in boxRadius + 1 until source.width - boxRadius) {
                    sum += source[rightX++, y]
                    sum -= source[leftX++, y]
                    target[targetX++, y] = sum / kernelSize
                }
                for (x in source.width - boxRadius until source.width) {
                    sum += last
                    sum -= source[leftX++, y]
                    target[targetX++, y] = sum / kernelSize
                }
            }
        }

        private fun boxBlurVertical(source: Matrix, target: Matrix, boxRadius: Int) {
            val kernelSize = boxRadius + boxRadius + 1

            for (x in 0 until source.width) {
                val first = source[x, 0]
                val last = source[x, source.height - 1]

                var leftY = 0
                var rightY = boxRadius
                var targetY = 0

                var sum = first * (boxRadius + 1)
                for (y in 0 until boxRadius) {
                    sum += source[x, y]
                }
                for (y in 0..boxRadius) {
                    sum += source[x, rightY++] - first
                    target[x, targetY++] = sum / kernelSize
                }
                for (y in boxRadius + 1 until source.height - boxRadius) {
                    sum += source[x, rightY++]
                    sum -= source[x, leftY++]

                    target[x, targetY++] = sum / kernelSize
                }
                for (y in source.height - boxRadius until source.height) {
                    sum += last
                    sum -= source[x, leftY++]

                    target[x, targetY++] = sum / kernelSize
                }
            }
        }

        private fun boxSizesForGauss(sigma: Double, n: Int): DoubleArray {
            val wIdeal = sqrt((12 * sigma * sigma / n) + 1)
            var wl = floor(wIdeal)
            if (wl % 2 == 0.0) wl--
            val wu: Double = wl + 2
            val mIdeal: Double = ((12 * sigma * sigma) - (n * wl * wl) - (4 * n * wl) - (3 * n)) / (-4 * wl - 4)
            val m: Long = mIdeal.roundToLong()
            val sizes = DoubleArray(n)
            for (i in 0 until n) {
                sizes[i] = if (i < m) wl else wu
            }
            return sizes
        }
    }
}
