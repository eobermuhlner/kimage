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
    private val fastErrorThreshold: Double = 1.1,
    private val channel: Channel = Channel.Red
) : ImageAligner {

    constructor(radius: Int) : this(radius, radius)

    override fun align(base: Image, image: Image, centerX: Int, centerY: Int, maxOffset: Int): ImageAligner.Alignment {
        if (base === image) {
            return ImageAligner.Alignment(0, 0, 0.0)
        }

        val baseCropped1 = base.crop(centerX-fastRadiusX, centerY-fastRadiusY, fastRadiusX*2+1, fastRadiusY*2+1, false)
        val baseCropped2 = base.crop(centerX-radiusX, centerY-radiusY, radiusX*2+1, radiusY+1, false)

        var bestError1 = 1.0
        var bestError2 = 1.0

        var bestAlignX = 0
        var bestAlignY = 0
        for (dy in -maxOffset .. maxOffset) {
            for (dx in -maxOffset .. maxOffset) {
                val x = centerX + dx
                val y = centerY + dy
                val error1 = baseCropped1.averageError(image.crop(x-fastRadiusX, y-fastRadiusY, fastRadiusX*2+1, fastRadiusY+1, false), channel)
                if (error1 < bestError1 * fastErrorThreshold) {
                    //println("Error1: $dx, $dy : $error1")
                    val error2 = baseCropped2.averageError(image.crop(x-radiusX, y-radiusY, radiusX*2+1, radiusY*2+1, false), channel)
                    if (error2 < bestError2) {
                        println("Error2: $dx, $dy : $error2")
                        bestAlignX = dx
                        bestAlignY = dy
                        bestError1 = error1
                        bestError2 = error2
                    }
                }
            }
        }

        return ImageAligner.Alignment(bestAlignX, bestAlignY, bestError2)
    }
}