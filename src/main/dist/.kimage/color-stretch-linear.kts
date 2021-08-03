import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.matrix.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "color-stretch-linear"
    title = "Linear stretching of colors"
    description = """
                Stretch the pixel values linearly so that the entire color range is used.
                """
    arguments {
        double("low") {
            description = """
                Low percentile that will be stretched to 0.
                Values below this percentile will be clipped at 1.
                """
            unit = "% percentile"
            default = 0.1
        }
        double("high") {
            description = """
                High percentile that will be stretched to 1.
                Values above this percentile will be clipped at 1.
                """
            unit = "% percentile"
            default = 99.9
        }
        string("channel") {
            description = """
                Channel used to measure the values to be stretched.
                """
            allowed = listOf("RGB", "Red", "Green", "Blue", "Luminance", "Gray")
            default = "RGB"
        }
    }

    single {
        val low: Double by arguments
        val high: Double by arguments
        val channel: String by arguments

        val channels = when (channel) {
            "Gray" -> listOf(Channel.Gray)
            "Luminance" -> listOf(Channel.Luminance)
            "Red" -> listOf(Channel.Red)
            "Green" -> listOf(Channel.Green)
            "Blue" -> listOf(Channel.Blue)
            "RGB" -> listOf(Channel.Red, Channel.Green, Channel.Blue)
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }

        val histogram = Histogram()
        for (measureChannel in channels) {
            histogram.add(inputImage[measureChannel])
        }

        val lowValue = histogram.estimatePercentile(low / 100.0)
        val highValue = histogram.estimatePercentile(high / 100.0)

        val range = highValue - lowValue

        if (verboseMode) {
            println("Low value:  $lowValue")
            println("High value: $highValue")
        }

        val outputMatrices = mutableMapOf<Channel, Matrix>()
        for (processChannel in inputImage.channels) {
            println("Processing channel: $processChannel")

            val matrix = inputImage[processChannel]

            val m = matrix.create()
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    val value = matrix[x, y]
                    m[x, y] = (value - lowValue) / range
                }
            }

            outputMatrices[processChannel] = m
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
    }
}
