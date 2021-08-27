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
        string("replace") {
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
                        The radius used in the replace method `local-median` to replace an outlier value. 
                        """
            enabledWhen = Reference("replace").isEqual("local-median")
            min = 1
            default = 10
        }
    }

    single {
        val kappa: Double by arguments
        var low: Optional<Double> by arguments
        var high: Optional<Double> by arguments
        val replace: String by arguments
        val localRadius: Int by arguments

        val badpixels: MutableSet<Pair<Int, Int>> = mutableSetOf()
        val badpixelMatrices = mutableMapOf<Channel, Matrix>()
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

            if (verboseMode) {
                println("Median value: $globalMedian")
                println("Standard deviation: $globalStddev")
                println("Clipping range: $low .. $high")
            }

            var outlierCount = 0
            val outputMatrix = matrix.create()
            val badpixelMatrix = matrix.create()
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    val value = matrix[x, y]
                    outputMatrix[x, y] = if (value in low.get() .. high.get()) {
                        matrix[x, y]
                    } else {
                        badpixels.add(Pair(x, y))
                        outlierCount++
                        val replacedValue = when (replace) {
                            "global-median" -> globalMedian
                            "local-median" -> matrix.medianAround(x, y, localRadius)
                            else -> throw java.lang.IllegalArgumentException("Unknown replace method: $replace")
                        }
                        badpixelMatrix[x, y] = replacedValue
                        replacedValue
                    }
                }
            }
            println("Found $outlierCount outliers")
            println()

            badpixelMatrices[channel] = badpixelMatrix
            outputMatrices[channel] = outputMatrix
        }

        val file = inputFile.prefixName(outputDirectory, "badpixels_").suffixExtension(".txt")
        println("Saving $file")
        val badpixelWriter = PrintWriter(FileWriter(file))

        for (badpixel in badpixels) {
            badpixelWriter.println(String.format("%6d %6d 0", badpixel.first, badpixel.second))
            if (debugMode) {
                val badPixelFile = inputFile.prefixName(outputDirectory, "badpixel_${badpixel.first}_${badpixel.second}_")
                val badPixelCrop = inputImage.cropCenter(5, badpixel.first, badpixel.second).scaleBy(4.0, 4.0, 0.0, 0.0, Scaling.Nearest)
                ImageWriter.write(badPixelCrop, badPixelFile)
            }
        }
        badpixelWriter.close()

        if (debugMode) {
            val badpixelImageFile = inputFile.prefixName(outputDirectory, "badpixel_")
            println("Saving $badpixelImageFile")
            val badpixelImage = MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> badpixelMatrices[channel]!! }
            ImageWriter.write(badpixelImage, badpixelImageFile)
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
    }
}
