import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import kotlin.math.*

// https://stats.stackexchange.com/questions/214877/is-there-a-formula-for-an-s-shaped-curve-with-domain-and-range-0-1
kimage(0.1) {
    name = "color-stretch-s-curve"
    title = "Stretch the colors non-linearly using a S-curve to fill the entire value range"
    description = """
                Stretch the colors non-linearly using a S-curve to fill the entire value range.
                """
    arguments {
        double("power") {
            description = """
                        The operation to use for merging.
                    """
            default = 0.5
        }
        string("midpoint") {
            allowed = listOf("histogram", "custom")
            default = "histogram"
        }
        double("percentile") {
            description = """
                        The percentile.
                    """
            default = 0.99
            enabledWhen = Reference("midpoint").isEqual("histogram")
        }
        double("midpointX") {
            description = """
                        The midpoint on the X axis.
                    """
            min = 0.0
            max = 1.0
            default = 0.5
            enabledWhen = Reference("midpoint").isEqual("custom")
        }
    }

    single {
        val power: Double by arguments
        val midpoint: String by arguments
        val percentile : Double by arguments
        var midpointX: Double by arguments

        midpointX = when (midpoint) {
            "histogram" -> {

                val histogram = Histogram()
                histogram.add(inputImage[Channel.Luminance])
                val percentileValue = histogram.estimatePercentile(percentile)
                percentileValue
            }
            "custom" -> midpointX
            else -> throw IllegalArgumentException("Unknown midpoint: $midpoint")
        }

        println("  final midpointX = $midpointX")

        val r = -ln(2.0) / ln(midpointX)

        val func: (Double) -> Double = { x ->
            //x.pow(power) / (x.pow(power) + (1.0 - x).pow(power))
            1.0/(1.0+(x.pow(r)/(1-x.pow(r))).pow(-power))
        }

        if (debugMode) {
            val curveImageSize = 1000
            val curveImage = MatrixImage(curveImageSize, curveImageSize)
            for (pixelX in 0 until curveImageSize) {
                val x = pixelX.toDouble() / curveImageSize
                val y = func(x)
                val pixelY = curveImageSize - (y * curveImageSize).toInt()
                curveImage[Channel.Red][pixelX, pixelY] = 1.0
                curveImage[Channel.Green][pixelX, pixelY] = 1.0
                curveImage[Channel.Blue][pixelX, pixelY] = 1.0
            }

            val curveFile = inputFile.prefixName(outputDirectory, "curve_")
            println("Saving $curveFile showing the color curve")
            ImageWriter.write(curveImage, curveFile)
            println()
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, width, height ->
            DoubleMatrix(width, height) { x, y -> clamp(func(inputImage[channel][x, y]), 0.0, 1.0) }
        }
    }
}
