package ch.obermuhlner.kimage.align

import ch.obermuhlner.astro.gradient.align.ImageAligner
import ch.obermuhlner.kimage.averageError
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image

class SimpleImageAligner(val radiusX1: Int = 50, val radiusY1: Int = 0, val radiusX2: Int = 100, val radiusY2: Int = 100, val errorThreshold: Double = 1.1, val channel: Channel = Channel.Red) :
    ImageAligner {

    constructor(radius: Int) : this(radius/2, 0, radius, radius)

    override fun align(base: Image, image: Image, centerX: Int, centerY: Int, maxOffset: Int): ImageAligner.Alignment {
        if (base === image) {
            return ImageAligner.Alignment(0, 0, 0.0)
        }

        //val baseCropped1 = base.croppedImage(centerX-radiusX1, centerY-radiusY1, radiusX1*2+1, radiusY1+1, false)
        val baseCropped2 = base.crop(centerX-radiusX2, centerY-radiusY2, radiusX2*2+1, radiusY2+1, false)

        //var bestError1 = 1.0
        var bestError2 = 1.0

        var bestAlignX = 0
        var bestAlignY = 0
        for (dy in -maxOffset .. maxOffset) {
            for (dx in -maxOffset .. maxOffset) {
                val x = centerX + dx
                val y = centerY + dy
//                val error1 = baseCropped1.averageError(image.croppedImage(x-radiusX1, y-radiusY1, radiusX1*2+1, radiusY1+1, false), channel)
//                if (error1 < bestError1 * errorThreshold) {
                    //println("Error1: $dx, $dy : $error1 better than $bestError1")
                    val error2 = baseCropped2.averageError(image.crop(x-radiusX2, y-radiusY2, radiusX2*2+1, radiusY2*2+1, false), channel)
                    if (error2 < bestError2) {
                        //println("Error2: $dx, $dy : $error2 better than $bestError2")
                        bestAlignX = dx
                        bestAlignY = dy
//                        bestError1 = error1
                        bestError2 = error2
                    }
//                }
            }
        }

        return ImageAligner.Alignment(bestAlignX, bestAlignY, bestError2)
    }
}