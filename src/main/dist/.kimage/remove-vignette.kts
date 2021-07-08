import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "remove-vignette"
    title = "Remove the vignette effect from image"
    description = """
                Calculates a statistical model of the vignette effect of the input image and removes it.
                """
    arguments {
        optionalInt("centerX") {
            description = """
                        The X coordinate of the center of the vignette effect.
                        The default value is the center of the image.
                        """
        }
        optionalInt("centerY") {
            description = """
                        The Y coordinate of the center of the vignette effect.
                        The default value is the center of the image.
                        """
        }
        string("mode") {
            description = """
                        Controls which channels are used to calculate the vignette effect.
                        The `rgb` mode calculates the effect on the three color channels separately.
                        """
            allowed = listOf("rgb", "gray", "luminance", "red", "green", "blue")
            default = "rgb"
        }
        double("kappa") {
            description = """
                        The kappa factor is used in sigma-clipping of sample values to determine the vignette effect.
                        """
            default = 2.0
        }
    }
    single {
        // based on http://rosettacode.org/wiki/Polynomial_regression#Kotlin
        fun polyRegression(x: DoubleArray, y: DoubleArray): DoubleArray {
            val xm = x.average()
            val ym = y.average()
            val x2m = x.map { it * it }.average()
            val x3m = x.map { it * it * it }.average()
            val x4m = x.map { it * it * it * it }.average()
            val xym = x.zip(y).map { it.first * it.second }.average()
            val x2ym = x.zip(y).map { it.first * it.first * it.second }.average()

            val sxx = x2m - xm * xm
            val sxy = xym - xm * ym
            val sxx2 = x3m - xm * x2m
            val sx2x2 = x4m - x2m * x2m
            val sx2y = x2ym - x2m * ym

            val b = (sxy * sx2x2 - sx2y * sxx2) / (sxx * sx2x2 - sxx2 * sxx2)
            val c = (sx2y * sxx - sxy * sxx2) / (sxx * sx2x2 - sxx2 * sxx2)
            val a = ym - b * xm - c * x2m

            val result = DoubleArray(3)
            result[0] = a
            result[1] = b
            result[2] = c
            return result
        }

        fun polynomialFunction(x: Double, powers: DoubleArray) = powers[0] + powers[1] * x + powers[2] * x * x

        val kappa: Double by arguments
        var centerX: Optional<Int> by arguments
        var centerY: Optional<Int> by arguments
        val mode: String by arguments

        if (!centerX.isPresent) {
            centerX = Optional.of(inputImage.width / 2)
        }
        if (!centerY.isPresent) {
            centerY = Optional.of(inputImage.height / 2)
        }

        println("Arguments (calculated from input):")
        println("  centerX = ${centerX.get()}")
        println("  centerY = ${centerY.get()}")
        println()

        val channels = when (mode) {
            "gray" -> listOf(Channel.Gray)
            "luminance" -> listOf(Channel.Luminance)
            "red" -> listOf(Channel.Red)
            "green" -> listOf(Channel.Green)
            "blue" -> listOf(Channel.Blue)
            "rgb" -> listOf(Channel.Red, Channel.Green, Channel.Blue)
            else -> throw IllegalArgumentException("Unknown channel mode: $mode")
        }
        val channelMatrices = mutableListOf<Matrix>()

        for (channelIndex in channels.indices) {
            val channel = channels[channelIndex]

            println("Processing $channel channel")
            println()

            val matrix = inputImage[channel]

            val calculatedMaxDistance = centerX.get() + centerY.get() // TODO calculate better
            val distanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }
            val clippedDistanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }

            val totalMedian = matrix.fastMedian()
            val totalStddev = matrix.stddev()
            val low = totalMedian - totalStddev * kappa
            val high = totalMedian + totalStddev * kappa

            if (verboseMode) {
                println("Median value: $totalMedian")
                println("Standard deviation: $totalStddev")
                println("Sigma clipping range: $low .. $high")
            }

            var maxDistance = 0
            var clippedMaxDistance = 0
            for (y in 0 until inputImage.height) {
                for (x in 0 until inputImage.width) {
                    val value = matrix.getPixel(x, y)
                    val dx = (centerX.get() - x).toDouble()
                    val dy = (centerY.get() - y).toDouble()
                    val distance = (sqrt(dx*dx + dy*dy) + 0.5).toInt()
                    distanceValues[distance].add(value.toFloat())
                    maxDistance = max(maxDistance, distance)
                    if (value in low..high) {
                        clippedDistanceValues[distance].add(value.toFloat())
                        clippedMaxDistance = max(maxDistance, distance)
                    }
                }
            }

            val xValues = mutableListOf<Double>()
            val yValues = mutableListOf<Double>()
            for (i in 0 until clippedMaxDistance) {
                val median = clippedDistanceValues[i].toFloatArray().medianInplace()
                if (median.isFinite()) {
                    xValues.add(i.toDouble())
                    yValues.add(median.toDouble())

                    xValues.add(-i.toDouble())
                    yValues.add(median.toDouble())
                }
            }
            println("Samples for regression analysis: ${xValues.size}")

            val polynomialPowers = polyRegression(xValues.toDoubleArray(), yValues.toDoubleArray())
            println("Polynomial function: y = ${polynomialPowers[0]} + ${polynomialPowers[1]}x + ${polynomialPowers[2]}x^2")

            var error = 0.0
            for (i in xValues.indices) {
                val y1 = yValues[i]
                val y2 = polynomialFunction(xValues[i], polynomialPowers)
                val delta = y1 - y2
                error += delta * delta
            }
            error /= xValues.size
            println("Standard Error: $error")

            val flatMatrix = CalculatedMatrix(inputImage.width, inputImage.height) { row, column ->
                val dx = (centerX.get() - column).toDouble()
                val dy = (centerY.get() - row).toDouble()
                val distance = sqrt(dx*dx + dy*dy)
                polynomialFunction(distance, polynomialPowers).toDouble()
            }

            val flatMax = flatMatrix.max()
            val normalizedFlatMatrix = flatMatrix.copy().onEach { v -> v / flatMax }

            channelMatrices.add(normalizedFlatMatrix)

            println()
        }

        var flatImage = MatrixImage(inputImage.width, inputImage.height,
            Channel.Red to channelMatrices[0],
            Channel.Green to channelMatrices[min(1, channelMatrices.size-1)],
            Channel.Blue to channelMatrices[min(2, channelMatrices.size-1)])

        if (debugMode) {
            val flatFile = inputFile.prefixName("flat_")
            println("Saving $flatFile for manual analysis")
            ImageWriter.write(flatImage, flatFile)
            println()
        }

        inputImage / flatImage
    }
}
