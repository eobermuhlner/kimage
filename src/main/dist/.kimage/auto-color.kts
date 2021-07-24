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
        double("kappa") {
            description = """
                        The kappa factor is used in sigma-clipping of sample values to determine the outlier values.
                        """
            default = 10.0
        }
        optionalDouble("low") {
            description = """
                        The low threshold to remove outliers below.
                        The default `low` value is calculated from the image using the `kappa` factor.
                        """
        }
        optionalDouble("high") {
            description = """
                        The high threshold to remove outliers below.
                        The default `high` value is calculated from the image using the `kappa` factor.
                        """
        }
    }

    single {
        val kappa: Double by arguments
        var low: Optional<Double> by arguments
        var high: Optional<Double> by arguments

        val outputMatrices = mutableMapOf<Channel, Matrix>()
        for (channel in inputImage.channels) {
            println("Processing channel: $channel")

            val matrix = inputImage[channel]

            val globalMedian = matrix.fastMedian()
            val globalStddev = matrix.stddev()
            if (!low.isPresent) {
                low = Optional.of(globalMedian - globalStddev * kappa)
            }
            if (!high.isPresent) {
                high = Optional.of(globalMedian + globalStddev * kappa)
            }

            val range = high.get() - low.get()


            if (verboseMode) {
                println("Median value: $globalMedian")
                println("Standard deviation: $globalStddev")
                println("Clipping range: $low .. $high")
            }

            val m = matrix.create()
            for (row in 0 until matrix.rows) {
                for (column in 0 until matrix.columns) {
                    val value = matrix[row, column]
                    m[row, column] = (value - low.get()) / range
                }
            }

            outputMatrices[channel] = m
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
    }
}