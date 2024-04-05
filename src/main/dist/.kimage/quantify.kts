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
    name = "quantify"
    title = "Quantify image values"
    description = """
                Quantifies the image values by rounding each value to the nearest step.
                """
    arguments {
        double("step") {
            description = """
                        The step to quantify.
                    """
            default = 0.1
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
        val step: Double by arguments
        val channel: String by arguments

        val channel2 = when (channel) {
            "gray" -> Channel.Gray
            "luminance" -> Channel.Luminance
            "red" -> Channel.Red
            "green" -> Channel.Green
            "blue" -> Channel.Blue
            "rgb" -> null
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }

        if (channel2 == null) {
            // all channels
            MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, width, height ->
                DoubleMatrix(width, height) { x, y ->
                    (inputImage[channel][x, y] / step).roundToInt().toDouble() * step
                }
            }
        } else {
            val imageMatrix = inputImage[channel2]
            MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, width, height ->
                DoubleMatrix(width, height) { x, y ->
                    (imageMatrix[x, y] / step).roundToInt().toDouble() * step
                }
            }
        }
    }
}
