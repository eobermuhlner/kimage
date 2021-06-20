package ch.obermuhlner.kimage.align

import ch.obermuhlner.astro.gradient.align.ImageAligner
import ch.obermuhlner.kimage.averageError
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image

class SimpleImageAligner(
    private val radiusX: Int = 100,
    private val radiusY: Int = 100,
    private val fastRadiusX: Int = radiusX,
    private val fastRadiusY: Int = 0,
    private val fastErrorThreshold: Double = 1.5,
) : ImageAligner {

    constructor(radius: Int) : this(radius, radius)

    override fun findInterestingCropCenter(image: Image): Pair<Int, Int> {
        val insetWidth = image.width / 4
        val insetHeight = image.height / 4
        val radiusStepX = radiusX / 10
        val radiusStepY = radiusY / 10
        var bestStdDev = 0.0
        var bestX = 0
        var bestY = 0
        for (y in insetWidth until image.height - insetWidth step radiusStepY) {
            for (x in insetHeight until image.width - insetHeight step radiusStepX) {
                val croppedImage = image.cropCenter(x, y, radiusX, radiusY)
                val stddev = croppedImage[Channel.Red].stddev()
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

    override fun align(base: Image, image: Image, centerX: Int, centerY: Int, maxOffset: Int): ImageAligner.Alignment {
        if (base === image) {
            return ImageAligner.Alignment(0, 0, 0.0)
        }

        val baseCropped0 = base.crop(centerX, centerY, 1, 1, false)
        val baseCropped1 = base.crop(centerX-fastRadiusX, centerY-fastRadiusY, fastRadiusX*2+1, fastRadiusY*2+1, false)
        val baseCropped2 = base.crop(centerX-radiusX, centerY-radiusY, radiusX*2+1, radiusY+1, false)

        var bestError0 = Double.MAX_VALUE
        var bestError1 = Double.MAX_VALUE
        var bestError2 = Double.MAX_VALUE

        var bestAlignX = 0
        var bestAlignY = 0
        for (dy in -maxOffset .. maxOffset) {
            for (dx in -maxOffset .. maxOffset) {
                val x = centerX + dx
                val y = centerY + dy
                val crop0 = image.crop(x, y, 1, 1, false)
                val error0 = baseCropped0.averageError(crop0)
                if (error0 < bestError0 * fastErrorThreshold) {
                    //println("Error0: $dx, $dy : $error0")
                    val crop1 = image.crop(x-fastRadiusX, y-fastRadiusY, fastRadiusX*2+1, fastRadiusY+1, false)
                    val error1 = baseCropped1.averageError(crop1)
                    if (error1 < bestError1 * fastErrorThreshold) {
                        //println("Error1: $dx, $dy : $error1")
                        val crop2 = image.crop(x - radiusX, y - radiusY, radiusX * 2 + 1, radiusY * 2 + 1, false)
                        val error2 = baseCropped2.averageError(crop2)
                        if (error2 < bestError2) {
                            //println("Error2: $dx, $dy : $error2")
                            bestAlignX = dx
                            bestAlignY = dy
                            bestError1 = error1
                            bestError2 = error2
                        }
                    }
                }
            }
        }

        return ImageAligner.Alignment(bestAlignX, bestAlignY, bestError2)
    }
}