package ch.obermuhlner.kimage.align

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.max

class ImageAligner(
    private val radiusX: Int = 100,
    private val radiusY: Int = 100,
    private val fastRadiusX: Int = radiusX,
    private val fastRadiusY: Int = radiusY,
    private val fastErrorFactor: Double = 1.1,
    private val fastErrorConstant: Double = 0.0001
) {

    constructor(radius: Int) : this(radius, radius)

    fun findInterestingCropCenter(image: Image, insetFactor: Int = 4, stepFactor: Int = 10): Pair<Int, Int> {
        val insetWidth = image.width / insetFactor
        val insetHeight = image.height / insetFactor
        val radiusStepX = max(radiusX / stepFactor, 1)
        val radiusStepY = max(radiusY / stepFactor, 1)
        var bestStdDev = 0.0
        var bestX = 0
        var bestY = 0
        for (y in insetWidth until image.height - insetWidth step radiusStepY) {
            for (x in insetHeight until image.width - insetHeight step radiusStepX) {
                val croppedImage = image.cropCenter(radiusX, radiusY, x, y)
                val stddev = croppedImage.values().stddev()
                if (stddev > bestStdDev) {
                    //ImageWriter.write(croppedImage, File("check_${stddev}.png"))
                    //println("stddev $x, $y : $stddev")
                    bestStdDev = stddev
                    bestX = x
                    bestY = y
                }
            }
        }

        return Pair(bestX, bestY)
    }

    fun align(base: Image, image: Image, centerX: Int, centerY: Int, maxOffset: Int, subPixelStep: Double = 0.1, createErrorMatrix: Boolean = false): Alignment {
        if (base === image) {
            return Alignment(0, 0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0, null, null, null, null)
        }

        val errorMatrix0: Matrix? = if (createErrorMatrix) {
            Matrix.matrixOf(base.width, base.height) { _, _ -> -1.0 }
        } else {
            null
        }
        val errorMatrix1: Matrix? = if (createErrorMatrix) {
            Matrix.matrixOf(base.width, base.height) { _, _ -> -1.0 }
        } else {
            null
        }
        val errorMatrix2: Matrix? = if (createErrorMatrix) {
            Matrix.matrixOf(base.width, base.height) { _, _ -> -1.0 }
        } else {
            null
        }
        val errorMatrix3: Matrix? = if (createErrorMatrix) {
            Matrix.matrixOf(base.width, base.height) { _, _ -> -1.0 }
        } else {
            null
        }

        val baseCropped0 = base.crop(centerX, centerY, 1, 1, false)
        val baseCropped1 = base.crop(centerX-fastRadiusX, centerY, fastRadiusX*2+1, 1, false)
        val baseCropped2 = base.crop(centerX, centerY-fastRadiusY, 1, fastRadiusY*2+1, false)
        val baseCropped3 = base.crop(centerX-radiusX, centerY-radiusY, radiusX*2+1, radiusY*2+1, false)

        var bestError0 = Double.MAX_VALUE
        var bestError1 = Double.MAX_VALUE
        var bestError2 = Double.MAX_VALUE
        var bestError3 = Double.MAX_VALUE

        var hitCountError0 = 0
        var hitCountError1 = 0
        var hitCountError2 = 0
        var hitCountError3 = 0
        var hitCountError4 = 0

        var bestAlignX = 0
        var bestAlignY = 0
        for (dy in -maxOffset .. maxOffset) {
            for (dx in -maxOffset..maxOffset) {
                val x = centerX + dx
                val y = centerY + dy
                val crop0 = image.crop(x, y, 1, 1, false)
                val crop1 = image.crop(x - fastRadiusX, y, fastRadiusX*2 + 1, 1, false)
                val crop2 = image.crop(x, y - fastRadiusY, 1, fastRadiusY*2 + 1, false)
                val crop3 = image.crop(x - radiusX, y - radiusY, radiusX*2 + 1, radiusY*2 + 1, false)

                val error0 = baseCropped0.averageError(crop0)
                if (errorMatrix0 != null) {
                    errorMatrix0[x, y] = error0
                }
                if (error0 < bestError0 * fastErrorFactor + fastErrorConstant) {
                    //println("Error0: $dx, $dy : $error0")
                    hitCountError0++
                    bestError0 = error0
                    val error1 = baseCropped1.averageError(crop1)
                    if (errorMatrix1 != null) {
                        errorMatrix1[x, y] = error1
                    }
                    if (error1 < bestError1 * fastErrorFactor + fastErrorConstant) {
                        //println("Error1: $dx, $dy : $error1")
                        hitCountError1++
                        bestError1 = error1
                        val error2 = baseCropped2.averageError(crop2)
                        if (errorMatrix2 != null) {
                            errorMatrix2[x, y] = baseCropped2.averageError(crop2)
                        }
                        if (error2 < bestError2 * fastErrorFactor + fastErrorConstant) {
                            hitCountError2++
                            bestError2 = error2
                            //println("Error2: $dx, $dy : $error2")
                            val error3 = baseCropped3.averageError(crop3)
                            if (errorMatrix3 != null) {
                                errorMatrix3[x, y] = error3
                            }
                            if (error3 < bestError3) {
                                hitCountError3++
                                //println("Error3: $dx, $dy : $error3")
                                bestAlignX = dx
                                bestAlignY = dy

                                bestError3 = error3
                            }
                        }
                    }
                }
//                if (errorMatrix0 != null) {
//                    errorMatrix0[x, y] = baseCropped0.averageError(crop0)
//                }
//                if (errorMatrix1 != null) {
//                    errorMatrix1[x, y] = baseCropped1.averageError(crop1)
//                }
//                if (errorMatrix2 != null) {
//                    errorMatrix2[x, y] = baseCropped2.averageError(crop2)
//                }
//                if (errorMatrix3 != null) {
//                    errorMatrix3[x, y] = baseCropped3.averageError(crop3)
//                }
            }
        }

        var subPixelAlignX = 0.0
        var subPixelAlignY = 0.0
        var bestError4 = bestError3
        if (subPixelStep != 0.0) {
            val crop = image.crop(centerX + bestAlignX - radiusX, centerY + bestAlignY - radiusY, radiusX * 2 + 1, radiusY * 2 + 1, false)

            var dy = -1.0 + subPixelStep
            while (dy < 1.0) {
                var dx = -1.0 + subPixelStep
                while (dx < 1.0) {
                    val subCrop = crop.scaleBy(1.0, 1.0, dx, dy)
                    val error4 = baseCropped3.averageError(subCrop)
                    if (error4 < bestError4) {
                        //println("Error4: $dx, $dy : $error4")
                        hitCountError4++
                        bestError4 = error4
                        subPixelAlignX = dx
                        subPixelAlignY = dy
                    }

                    dx += subPixelStep
                }
                dy += subPixelStep
            }
        }

        return Alignment(bestAlignX, bestAlignY, subPixelAlignX, subPixelAlignY, bestError4, hitCountError0, hitCountError1, hitCountError2, hitCountError3, hitCountError4, errorMatrix0, errorMatrix1, errorMatrix2, errorMatrix3)
    }
}

data class Alignment(
    val x: Int,
    val y: Int,
    val subPixelX: Double,
    val subPixelY: Double,
    val error: Double,
    val hitCountError0: Int,
    val hitCountError1: Int,
    val hitCountError2: Int,
    val hitCountError3: Int,
    val hitCountError4: Int,
    val errorMatrix0: Matrix?,
    val errorMatrix1: Matrix?,
    val errorMatrix2: Matrix?,
    val errorMatrix3: Matrix?

)
