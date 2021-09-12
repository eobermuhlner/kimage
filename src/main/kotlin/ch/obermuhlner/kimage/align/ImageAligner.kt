package ch.obermuhlner.kimage.align

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.math.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class ImageAligner(
    private val radiusX: Int = 100,
    private val radiusY: Int = 100,
    private val fastRadiusX: Int = radiusX,
    private val fastRadiusY: Int = 0,
    private val fastErrorThreshold: Double = 1.5,
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

    fun align(base: Image, image: Image, centerX: Int, centerY: Int, maxOffset: Int, subPixelStep: Double = 0.1): Alignment = runBlocking {
        if (base === image) {
            return@runBlocking Alignment(0, 0, 0.0, 0.0, 0.0, 0, 0, 0)
        }

        val baseCropped0 = base.crop(centerX, centerY, 1, 1, false)
        val baseCropped1 = base.crop(centerX-fastRadiusX, centerY-fastRadiusY, fastRadiusX*2+1, fastRadiusY*2+1, false)
        val baseCropped2 = base.crop(centerX-radiusX, centerY-radiusY, radiusX*2+1, radiusY+1, false)

        val mutex = Mutex()
        var bestError0 = Double.MAX_VALUE
        var bestError1 = Double.MAX_VALUE
        var bestError2 = Double.MAX_VALUE

        var hitCountError0 = 0
        var hitCountError1 = 0
        var hitCountError2 = 0

        var bestAlignX = 0
        var bestAlignY = 0
        for (dy in -maxOffset .. maxOffset) {
            //launch(Dispatchers.Default) {
                for (dx in -maxOffset..maxOffset) {
                    val x = centerX + dx
                    val y = centerY + dy
                    val crop0 = image.crop(x, y, 1, 1, false)
                    val error0 = baseCropped0.averageError(crop0)
                    if (error0 < bestError0 * fastErrorThreshold) {
                        //println("Error0: $dx, $dy : $error0")
                        hitCountError0++
                        val crop1 = image.crop(x - fastRadiusX, y - fastRadiusY, fastRadiusX * 2 + 1, fastRadiusY + 1, false)
                        val error1 = baseCropped1.averageError(crop1)
                        if (error1 < bestError1 * fastErrorThreshold) {
                            //println("Error1: $dx, $dy : $error1")
                            hitCountError1++
                            val crop2 = image.crop(x - radiusX, y - radiusY, radiusX * 2 + 1, radiusY * 2 + 1, false)
                            val error2 = baseCropped2.averageError(crop2)
                            if (error2 < bestError2) {
                                hitCountError2++
                                //println("Error2: $dx, $dy : $error2")
                                bestAlignX = dx
                                bestAlignY = dy
                                mutex.withLock {
                                    bestError0 = error0
                                    bestError1 = error1
                                    bestError2 = error2
                                }
                            }
                        }
                    }
                }
            //}
        }

        var subPixelAlignX = 0.0
        var subPixelAlignY = 0.0
        if (subPixelStep != 0.0) {
            val crop = image.crop(centerX + bestAlignX - radiusX, centerY + bestAlignY - radiusY, radiusX * 2 + 1, radiusY * 2 + 1, false)
            var bestError3 = bestError2

            var dy = -1.0 + subPixelStep
            while (dy < 1.0) {
                var dx = -1.0 + subPixelStep
                while (dx < 1.0) {
                    val subCrop = crop.scaleBy(1.0, 1.0, dx, dy)
                    val error3 = baseCropped2.averageError(subCrop)
                    if (error3 < bestError3) {
                        //println("Error3: $dx, $dy : $error3")
                        subPixelAlignX = dx
                        subPixelAlignY = dy
                        bestError3 = error3
                    }

                    dx += subPixelStep
                }
                dy += subPixelStep
            }
        }

        Alignment(bestAlignX, bestAlignY, subPixelAlignX, subPixelAlignY, bestError2, hitCountError0, hitCountError1, hitCountError2)
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
    val hitCountError2: Int
)
