import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "crop-brightest"
    title = "Crop brightest patch"
    description = """
                Crops the brightest part of an image.
                """
    arguments {
        double("percentile") {
            min = 0.0
            max = 100.0
            unit = "% percentile"
            default = 80.0
        }
        string("channel") {
            description = """
                        """
            allowed = listOf("gray", "luminance", "red", "green", "blue")
            default = "gray"
        }
        int("radius") {
            description = """
                        The radius around the center of the brightest patch to crop.
                        """
            min = 1
        }
    }

    single {
        val percentile: Double by arguments
        val channel: String by arguments
        val radius: Int by arguments

        val measureChannel = when (channel) {
            "gray" -> Channel.Gray
            "luminance" -> Channel.Luminance
            "red" -> Channel.Red
            "green" -> Channel.Green
            "blue" -> Channel.Blue
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }

        val measureMatrix = inputImage[measureChannel]

        val histogram = Histogram()
        histogram.add(measureMatrix)
        val percentileValue = histogram.estimatePercentile(percentile / 100.0)

        var largestPatchWidth = 0
        var largestPatchX = 0
        var largestPatchY = 0

        for (y in 0 until inputImage.height) {
            var insidePatch = false
            var patchStartX = 0
            for (x in 0 until inputImage.width) {
                if (measureMatrix[x, y] >= percentileValue) {
                    if (!insidePatch) {
                        patchStartX = x
                        insidePatch = true
                    }
                } else {
                    if (insidePatch) {
                        val patchWidth = x - patchStartX
                        if (patchWidth > largestPatchWidth) {
                            largestPatchWidth = patchWidth
                            largestPatchX = patchStartX + largestPatchWidth/2
                            largestPatchY = y
                        }
                        insidePatch = false
                    }
                }
            }
        }

        println("Largest patch:")
        println("  X: $largestPatchX (width $largestPatchWidth)")
        println("  Y: $largestPatchY")
        println()

        if (debugMode) {
            val debugImage = inputImage.copy()
            for (x in 0 until largestPatchX-largestPatchWidth/2) {
                debugImage[Channel.Red][x, largestPatchY] = 1.0
            }
            for (x in largestPatchX+largestPatchWidth/2 until debugImage.width) {
                debugImage[Channel.Red][x, largestPatchY] = 1.0
            }
            val debugFile = inputFile.prefixName(outputDirectory, "debug_patch_")
            println("Saving $debugFile for manual analysis")
            ImageWriter.write(debugImage, debugFile)
            println()
        }

        inputImage.cropCenter(radius, largestPatchX, largestPatchY)
    }
}
