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
    title = "Crop largest brightest patch"
    description = """
                Crops the largest bright part of an image.
                
                The script searches the largest patch above the defined percentile on the x-axis and centers it on the y-axis.
                """
    arguments {
        double("percentile") {
            description = """
                        The percentile to determine the brightest patch.
                        """
            min = 0.0
            max = 100.0
            unit = "% percentile"
            default = 80.0
        }
        string("channel") {
            description = """
                        The channel used to find the largest bright patch.
                        """
            allowed = listOf("gray", "luminance", "red", "green", "blue")
            default = "gray"
        }
        int("radius") {
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

        inputImage.cropCenter(radius, largestPatchX, largestPatchY)
    }
}
