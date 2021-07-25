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
    name = "auto-color"
    title = "Automatically correct colors"
    description = """
                Stretch the pixel values so that the entire color range is used.
                """
    arguments {
        double("low") {
            default = 0.001
        }
        double("high") {
            default = 0.999
        }
    }

    single {
        val kappa: Double by arguments
        var low: Double by arguments
        var high: Double by arguments

        val measureMatrix = inputImage[Channel.Luminance]

        val histogram = Histogram()
        histogram.add(measureMatrix)
        val lowValue = histogram.estimatePercentile(low)
        val highValue = histogram.estimatePercentile(high)

        val range = highValue - lowValue

        if (verboseMode) {
            println("Low value:  $lowValue")
            println("High value: $highValue")
        }

        val outputMatrices = mutableMapOf<Channel, Matrix>()
        for (channel in inputImage.channels) {
            println("Processing channel: $channel")

            val matrix = inputImage[channel]

            val m = matrix.create()
            for (row in 0 until matrix.rows) {
                for (column in 0 until matrix.columns) {
                    val value = matrix[row, column]
                    m[row, column] = (value - lowValue) / range
                }
            }

            outputMatrices[channel] = m
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
    }
}
