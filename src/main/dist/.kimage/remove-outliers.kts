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
    name = "remove-outliers"
    title = "Remove outliers"
    description = """
                Rotate image 90 degrees left.
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
        string("method") {
            description = """
                        The method to replace the outlier values.
                        - `global-median` replaces outlier values with the global median of the current channel.
                          All outliers will be replaced with the same value.
                        - `local-median` replaces outlier values with the local median of the current channel
                          using the `local-radius`.
                        """
            allowed = listOf("global-median", "local-median")
            default = "global-median"
        }
        int("localRadius") {
            description = """
                        The radius used in the method `local-median` to replace an outlier value. 
                        """
            min = 1
            default = 10
        }
    }

    single {
        val kappa: Double by arguments
        var low: Optional<Double> by arguments
        var high: Optional<Double> by arguments
        val method: String by arguments
        val localRadius: Int by arguments

        val outputMatrices = mutableMapOf<Channel, Matrix>()
        for (channel in inputImage.channels) {
            println("Processing channel: $channel")

            val matrix = inputImage[channel]

            val globalMedian = matrix.fastMedian()
            val globalStddev = matrix.stddev()
            if (!low.isPresent) {
                low = Optional.of(globalMedian - globalStddev * kappa)
            }
            if (!low.isPresent) {
                low = Optional.of(globalMedian - globalStddev * kappa)
            }
            if (!high.isPresent) {
                high = Optional.of(globalMedian + globalStddev * kappa)
            }

            if (verboseMode) {
                println("Median value: $globalMedian")
                println("Standard deviation: $globalStddev")
                println("Clipping range: $low .. $high")
            }

            var outlierCount = 0
            val m = matrix.create()
            for (row in 0 until matrix.rows) {
                for (column in 0 until matrix.columns) {
                    val value = matrix[row, column]
                    m[row, column] = if (value in low.get() .. high.get()) {
                        matrix[row, column]
                    } else {
                        outlierCount++
                        when (method) {
                            "global-median" -> globalMedian
                            "local-median" -> matrix.medianAround(row, column, localRadius)
                            else -> throw java.lang.IllegalArgumentException("Unknown method: $method")
                        }

                    }
                }
            }
            println("Found $outlierCount outliers")
            println()

            outputMatrices[channel] = m
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
    }
}
