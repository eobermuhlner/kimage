import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "approach-reference"
    title = "Approaches a reference image"
    description = """
                Approaches an image to a reference image - using a function control how strong the difference are approaching.
                Useful to denoise a single sub-frame by approaching the stacked (and therefore denoised) image
                without losing image details that exist only in this frame (meteroids, asteroids).
                If all sub-frames are processed like this a video can be created that is the quality of the stack image but shows the details of the sub-frames. 
                """
    arguments {
        image("reference") {
            description = """
                        The reference image to approach.
                    """
        }
        string("function") {
            description = """
                        The function used to calculate how the image approaches the reference image.
                        """
            allowed = listOf("const", "power", "s-curve")
            default = "s-curve"
        }
        double("power") {
            description = """
                        The power of the difference to use for merging.
                    """
            default = 0.5
        }
        double("midpoint") {
            description = """
                        The midpoint to use for merging.
                    """
            default = 0.5
        }
        string("channel") {
            description = """
                        Controls which channels are used to calculate the difference.
                        The `rgb` channel calculates the effect on the three color channels separately.
                        """
            allowed = listOf("rgb", "gray", "luminance", "red", "green", "blue")
            default = "luminance"
        }
    }

    single {
        val reference: Image by arguments
        val function: String by arguments
        val power: Double by arguments
        val midpoint: Double by arguments
        val channel: String by arguments

        val croppedImage = inputImage.crop(0, 0, reference.width, reference.height, false)

        val constFunc: (Double) -> Double = { x ->
            power
        }

        val powerFunc: (Double) -> Double = { x ->
            x.pow(power)
        }

        val r = -ln(2.0) / ln(midpoint)

        val scurveFunc: (Double) -> Double = { x ->
            1.0/(1.0+(x.pow(r)/(1-x.pow(r))).pow(-power))
        }

        val factorFunc: (Double) -> Double = when (function) {
            "const" -> constFunc
            "power" -> powerFunc
            "s-curve" -> scurveFunc
            else -> throw IllegalArgumentException("Unknown function: $function")
        }

        val approachFunc: (Double, Double, Double) -> Double = { a, b, factor ->
            a + (b - a) * factor
        }

        val channel2 = when (channel) {
            "gray" -> Channel.Gray
            "luminance" -> Channel.Luminance
            "red" -> Channel.Red
            "green" -> Channel.Green
            "blue" -> Channel.Blue
            "rgb" -> null
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }

        if (debugMode) {
            val curveImageSize = 1000
            val curveImage = MatrixImage(curveImageSize, curveImageSize)
            for (pixelX in 0 until curveImageSize) {
                val x = pixelX.toDouble() / curveImageSize
                val y = factorFunc(x)
                val pixelY = curveImageSize - (y * curveImageSize).toInt()
                curveImage[Channel.Red][pixelX, pixelY] = 1.0
                curveImage[Channel.Green][pixelX, pixelY] = 1.0
                curveImage[Channel.Blue][pixelX, pixelY] = 1.0
            }

            val curveFile = inputFile.prefixName(outputDirectory, "curve_")
            println("Saving $curveFile showing the curve to mix the difference")
            ImageWriter.write(curveImage, curveFile)
            println()
        }

        if (channel2 == null) {
            // all channels
            MatrixImage(reference.width, reference.height, reference.channels) { channel, width, height ->
                DoubleMatrix(width, height) { x, y ->
                    val diff = croppedImage[channel][x, y] - reference[channel][x, y]
                    val factor = factorFunc(abs(diff)) * sign(diff)
                    clamp(approachFunc(reference[channel][x, y], croppedImage[channel][x, y], factor), 0.0, 1.0)
                }
            }
        } else {
            val referenceMatrix = reference[channel2]
            val imageMatrix = croppedImage[channel2]
            MatrixImage(reference.width, reference.height, reference.channels) { channel, width, height ->
                DoubleMatrix(width, height) { x, y ->
                    val diff = imageMatrix[x, y] - referenceMatrix[x, y]
                    val factor = factorFunc(abs(diff)) * sign(diff)
                    clamp(approachFunc(reference[channel][x, y], croppedImage[channel][x, y], factor), 0.0, 1.0)
                }
            }
        }
    }
}
