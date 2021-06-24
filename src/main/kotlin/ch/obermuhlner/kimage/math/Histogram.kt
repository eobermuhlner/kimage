package ch.obermuhlner.kimage.math

import ch.obermuhlner.kimage.matrix.Matrix

class Histogram(private val binCount: Int = 256) {
    private val bins = IntArray(binCount)
    private var n = 0

    val indices: IntRange = bins.indices

    operator fun get(index: Int) = bins[index]

    fun clear() {
        for (i in indices) {
            bins[i] = 0
        }
        n = 0
    }

    fun add(matrix: Matrix, rowStart: Int, columnStart: Int, rows: Int, columns: Int) {
        for (row in rowStart until rowStart+rows) {
            for (column in columnStart until columnStart+columns) {
                add(matrix[row, column])
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

    fun add(value: Double) {
        val index = clamp((value * binCount).toInt(), 0, binCount - 1)
        bins[index]++
        n++
    }

    fun remove(value: Double) {
        val index = clamp((value * binCount).toInt(), 0, binCount - 1)
        bins[index]--
        n--
    }

    fun estimateMean(): Double {
        var sum = 0.0
        for (i in indices) {
            sum += bins[i] * (i.toDouble() + 0.5) / (binCount - 1).toDouble()
        }
        return sum / n
    }

    fun estimateMedian(): Double {
        val nHalf = n / 2
        var cumulativeN = 0
        for (i in indices) {
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