package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.Scaling
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.Matrix.Companion.matrixOf
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun Matrix.contentToString(multiline: Boolean = false): String {
    val str = StringBuilder()

    str.append("[")
    for (y in 0 until height) {
        if (y != 0) {
            str.append(" ")
        }
        str.append("[")
        for (x in 0 until width) {
            if (x != 0) {
                str.append(" ,")
            }
            str.append(this[x, y])
        }
        str.append("]")
        if (multiline && y != height - 1) {
            str.appendLine()
        }
    }
    str.append("]")
    if (multiline) {
        str.appendLine()
    }

    return str.toString()
}

fun Matrix.contentEquals(other: Matrix, epsilon: Double = 1E-10): Boolean {
    if (height != other.height || width != other.width) {
        return false
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (abs(this[x, y] - other[x, y]) > epsilon) {
                return false
            }
        }
    }

    return true
}

fun max(m1: Matrix, m2: Matrix): Matrix {
    val m = m1.create()
    for (y in 0 until m1.height) {
        for (x in 0 until m1.width) {
            m[x, y] = kotlin.math.max(m1[x, y], m2[x, y])
        }
    }
    return m
}

fun Matrix.rotateLeft(): Matrix {
    val m = create(height, width)

    for (y in 0 until height) {
        for (x in 0 until width) {
            m[y, width - x - 1] = this[x, y]
        }
    }

    return m
}

fun Matrix.rotateRight(): Matrix {
    val m = create(height, width)

    for (y in 0 until height) {
        for (x in 0 until width) {
            m[height - y - 1, x] = this[x, y]
        }
    }

    return m
}

fun Matrix.mirrorX(): Matrix {
    val m = create(width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            m[width - x - 1, y] = this[x, y]
        }
    }

    return m
}

fun Matrix.mirrorY(): Matrix {
    val m = create(width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            m[x, height - y - 1] = this[x, y]
        }
    }

    return m
}

fun Matrix.stretchClassic(min: Double, max: Double, func: (value: Double) -> Double = { it }): Matrix {
    val denom = func(max - min)
    return this.copy().onEach { value ->
        when {
            value < min -> 0.0
            value > max -> 1.0
            else -> func(value - min) / denom
        }
    }
}

fun Matrix.averageError(other: Matrix): Double {
    var sum = 0.0
    for (index in 0 until size) {
        val delta = this[index] - other[index]
        sum += delta * delta
    }
    return sum / size
}

fun Matrix.scaleBy(scaleWidth: Double, scaleHeight: Double, offsetX: Double = 0.0, offsetY: Double = 0.0, scaling: Scaling = Scaling.Bicubic): Matrix {
    val newWidth = (width * scaleWidth).toInt()
    val newHeight = (height * scaleHeight).toInt()

    return scaleTo(newWidth, newHeight, offsetX, offsetY, scaling)
}

fun Matrix.scaleTo(newWidth: Int, newHeight: Int, offsetX: Double = 0.0, offsetY: Double = 0.0, scaling: Scaling = Scaling.Bicubic): Matrix {
    return when (scaling) {
        Scaling.Nearest -> scaleNearestTo(newWidth, newHeight, offsetX, offsetY)
        Scaling.Bilinear -> scaleBilinearTo(newWidth, newHeight, offsetX, offsetY)
        Scaling.Bicubic -> scaleBicubicTo(newHeight, newWidth, offsetX, offsetY)
    }
}

private fun Matrix.scaleNearestTo(newWidth: Int, newHeight: Int, offsetX: Double = 0.0, offsetY: Double = 0.0): Matrix {
    val m = create(newWidth, newHeight)
    for (newY in 0 until newHeight) {
        for (newX in 0 until newWidth) {
            val oldX = (newX.toDouble() / newWidth * width + offsetX).toInt()
            val oldY = (newY.toDouble() / newHeight * height + offsetY).toInt()

            val newValue = this[oldX, oldY]
            m[newX, newY] = newValue
        }
    }

    return m
}

private fun Matrix.scaleBilinearTo(newWidth: Int, newHeight: Int, offsetX: Double = 0.0, offsetY: Double = 0.0): Matrix {
    val m = create(newWidth, newHeight)
    for (newY in 0 until newHeight) {
        for (newX in 0 until newWidth) {
            val oldX = newX.toDouble() / newWidth * (width - 1) + offsetX + 0.5
            val oldY = newY.toDouble() / newHeight * (height - 1) + offsetY + 0.5
            val oldXInt = oldX.toInt()
            val oldYInt = oldY.toInt()
            val oldXFract = oldX - oldXInt
            val oldYFract = oldY - oldYInt

            val v00 = this[oldXInt, oldYInt]
            val v01 = this[oldXInt + 1, oldYInt]
            val v10 = this[oldXInt, oldYInt + 1]
            val v11 = this[oldXInt + 1, oldYInt + 1]

            val newValue = mixBilinear(v00, v01, v10, v11, oldYFract, oldXFract)

            m[newX, newY] = newValue
        }
    }

    return m
}

private fun Matrix.scaleBicubicTo(newHeight: Int, newWidth: Int, offsetX: Double = 0.0, offsetY: Double = 0.0): Matrix {
    val m = create(newWidth, newHeight)
    for (newY in 0 until newHeight) {
        for (newX in 0 until newWidth) {
            val oldX = newX.toDouble() / newWidth * (width - 1) + offsetX + 0.5
            val oldY = newY.toDouble() / newHeight * (height - 1) + offsetY + 0.5
            val oldXInt = oldX.toInt()
            val oldYInt = oldY.toInt()
            val oldXFract = oldX - oldXInt
            val oldYFract = oldY - oldYInt

            val v00 = this[oldXInt - 1, oldYInt - 1]
            val v10 = this[oldXInt - 1, oldYInt + 0]
            val v20 = this[oldXInt - 1, oldYInt + 1]
            val v30 = this[oldXInt - 1, oldYInt + 2]

            val v01 = this[oldXInt + 0, oldYInt - 1]
            val v11 = this[oldXInt + 0, oldYInt + 0]
            val v21 = this[oldXInt + 0, oldYInt + 1]
            val v31 = this[oldXInt + 0, oldYInt + 2]

            val v02 = this[oldXInt + 1, oldYInt - 1]
            val v12 = this[oldXInt + 1, oldYInt + 0]
            val v22 = this[oldXInt + 1, oldYInt + 1]
            val v32 = this[oldXInt + 1, oldYInt + 2]

            val v03 = this[oldXInt + 2, oldYInt - 1]
            val v13 = this[oldXInt + 2, oldYInt + 0]
            val v23 = this[oldXInt + 2, oldYInt + 1]
            val v33 = this[oldXInt + 2, oldYInt + 2]

            val col0 = mixCubicHermite(v00, v10, v20, v30, oldYFract)
            val col1 = mixCubicHermite(v01, v11, v21, v31, oldYFract)
            val col2 = mixCubicHermite(v02, v12, v22, v32, oldYFract)
            val col3 = mixCubicHermite(v03, v13, v23, v33, oldYFract)
            val newValue = mixCubicHermite(col0, col1, col2, col3, oldXFract)

            m[newX, newY] = clamp(newValue, 0.0, 1.0)
        }
    }

    return m
}

fun Matrix.interpolate(fixPoints: List<Pair<Int, Int>>, valueFunc: (Pair<Int, Int>) -> Double = { valueAt(
    this,
    it.first,
    it.second
) }, power: Double = estimatePowerForInterpolation(fixPoints.size)): Matrix {
    val fixValues = fixPoints.map { valueFunc(it) }
    return interpolate(fixPoints, fixValues, power)
}

fun Matrix.interpolate(fixPoints: List<Pair<Int, Int>>, fixValues: List<Double>, power: Double = estimatePowerForInterpolation(fixPoints.size)): Matrix {
    val m = create()

    for (y in 0 until height) {
        for (x in 0 until width) {
            m[x, y] = interpolate(x, y, fixPoints, fixValues, power)
        }
    }

    return m
}

fun valueAt(matrix: Matrix, x: Int, y: Int): Double {
    return matrix[x, y]
}

fun Matrix.medianAround(x: Int, y: Int, radius: Int = 10): Double {
    return crop(x - radius, y - radius, radius+radius+1, radius+radius+1).median()
}

private fun interpolate(
    x: Int,
    y: Int,
    fixPoints: List<Pair<Int, Int>>,
    fixValues: List<Double>,
    power: Double = estimatePowerForInterpolation(fixPoints.size)
): Double {
    require(fixPoints.size == fixValues.size)

    val distances = DoubleArray(fixPoints.size)
    var totalDistance = 0.0
    for (i in fixPoints.indices) {
        val fixX = fixPoints[i].first
        val fixY = fixPoints[i].second

        val dX = (x-fixX).toDouble()
        val dY = (y-fixY).toDouble()
        val distance = sqrt(dX*dX + dY*dY)
        distances[i] = distance
        totalDistance += distance
    }

    val factors = DoubleArray(fixPoints.size)
    var totalFactor = 0.0
    for (i in fixPoints.indices) {
        var factor = 1.0 - distances[i] / totalDistance
        factor = factor.pow(power)
        factors[i] = factor
        totalFactor += factor
    }

    var mixedValue = 0.0
    for (i in fixPoints.indices) {
        val fixValue = fixValues[i]
        val factor = factors[i] / totalFactor
        mixedValue += fixValue * factor
    }

    return mixedValue
}

fun estimatePowerForInterpolation(n: Int): Double = n.toDouble().pow(1.6)

fun Matrix.erode(kernelRadius: Int = 1): Matrix {
    val m = create(width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            var minValue = Double.MAX_VALUE

            for (ky in -kernelRadius..kernelRadius) {
                for (kx in -kernelRadius..kernelRadius) {
                    minValue = minOf(minValue, this[x+kx, y+ky])
                }
            }

            m[x, y] = minValue
        }
    }

    return m
}

fun Matrix.erode(kernel: Matrix, strength: Double = 1.0, repeat: Int = 1): Matrix {
    var m1 = create(width, height).onEach { x, y, _ ->  this[x, y] }
    var m2 = create(width, height)

    val kernelCenterX = kernel.width / 2
    val kernelCenterY = kernel.height / 2

    for (i in 0 until repeat) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                var minValue = Double.MAX_VALUE

                for (ky in 0..kernel.height) {
                    for (kx in 0..kernel.width) {
                        if (kernel[kx, ky] >= 1.0) {
                            minValue = minOf(minValue, m1[x + kx - kernelCenterX, y + ky - kernelCenterY])
                        }
                    }
                }

                m2[x, y] = m1[x, y] * (1.0 - strength) + minValue * strength
            }
        }

        val tmp = m1
        m1 = m2
        m2 = tmp
    }

    return m1
}

fun Matrix.processTiles(size: Int, overlap: Int = size / 4, gradient: (Double) -> Double = { x -> 1.0 - x }, func: (Matrix) -> Matrix): Matrix {
    require (size > overlap) { "Overlap too large" }

    val result = matrixOf(this.width, this.height)

    val overlay = matrixOf(this.width, this.height) { x, y ->
        val rx = if (x < overlap) {
            (overlap - x.toDouble()) / overlap
        } else if (x > size - overlap) {
            (overlap - (size-x).toDouble()) / overlap
        } else {
            0.0
        }
        val ry = if (y < overlap) {
            (overlap - y.toDouble()) / overlap
        } else if (y > size - overlap) {
            (overlap - (size-y).toDouble()) / overlap
        } else {
            0.0
        }
        clamp(gradient(rx), 0.0, 1.0) * clamp(gradient(ry), 0.0, 1.0)
    }

    val step = size - overlap
    for (tileY in -overlap until this.height step step) {
        for (tileX in -overlap until this.width step step) {
            val cropped = this.crop(tileX, tileY, size, size)
            val processed = func(cropped)
            val m = processed elementTimes overlay
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val xx = tileX + x
                    val yy = tileY + y
                    if (result.isInside(xx, yy)) {
                        result[xx, yy] = clamp(result[xx, yy] + m[x, y], 0.0, 1.0)
                    }
                }
            }
        }
    }

    return result
}

