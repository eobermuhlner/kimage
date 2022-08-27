import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import ch.obermuhlner.kimage.astro.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "star-mask"
    title = "Create a star mask."
    description = """
                Create a mask image based on the detected stars.
                """
    arguments {
        int("blur") {
            min = 0
            default = 0
        }
        boolean("invert") {
            default = false
        }
    }

    single {
        val blur: Int by arguments
        val invert: Boolean by arguments

        var matrix = inputImage[Channel.Gray]
        val detector = StarDetector(matrix)
        val stars = detector.detectPotentialStars()
        println("Found ${stars.size} stars")

        val analyzedStars = detector.analyzePotentialStars(stars)

        if (debugMode) {
            val red = matrix.copy()
            val green = matrix.copy()
            val blue = matrix.copy()
            analyzedStars.forEach {
                val starRadius = it.radius.toInt()
                val x = (it.x + 0.5).toInt()
                val y = (it.y + 0.5).toInt()
                if (it.valid) {
                    for (dy in -starRadius .. starRadius) {
                        for (dx in -starRadius .. starRadius) {
                            if (dx*dx+dy*dy < starRadius*starRadius) {
                                blue[x+dx, y+dy] = 1.0
                            }
                        }
                    }
                }
                red[x, y] = if (it.valid) 0.0 else 1.0
                green[x, y] = if (it.valid) 1.0 else 0.0
                blue[x, y] = 0.0
            }
            val image = MatrixImage(red, green, blue)

            val starDebugFile = inputFile.prefixName(outputDirectory, "star_debug_")
            println("Saving $starDebugFile for manual analysis")
            ImageWriter.write(image, starDebugFile)
            println()
        }


        var starsMatrix: Matrix = DoubleMatrix(matrix.width, matrix.height)
        analyzedStars.forEach {
            val starRadius = it.radius.toInt()
            val x = (it.x + 0.5).toInt()
            val y = (it.y + 0.5).toInt()
            if (it.valid) {
                for (dy in -starRadius .. starRadius) {
                    for (dx in -starRadius .. starRadius) {
                        if (dx*dx+dy*dy < starRadius*starRadius) {
                            starsMatrix[x+dx, y+dy] = 1.0
                        }
                    }
                }
            }
        }

        if (blur > 0) {
            starsMatrix = starsMatrix.gaussianBlurFilter(blur)
        }
        if (invert) {
            starsMatrix.onEach { _, _, value ->
                1.0 - value
            }
        }

        MatrixImage(starsMatrix)
    }
}
