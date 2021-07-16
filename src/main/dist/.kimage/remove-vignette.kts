import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import org.apache.commons.math3.fitting.*

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
            hint = Hint.ImageX
        }
        optionalInt("centerY") {
            description = """
                        The Y coordinate of the center of the vignette effect.
                        The default value is the center of the image.
                        """
            hint = Hint.ImageY
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
        fun polynomialFunction(x: Double, coefficients: DoubleArray): Double {
            var xPower = 1.0
            var sum = 0.0
            for (coefficient in coefficients) {
                sum += coefficient * xPower
                xPower = xPower * x
            }
            return sum
        }
        fun gaussFunction(x: Double, amplitude: Double = 1.0, mean: Double = 0.0, sigma: Double = 1.0): Double {
            val dx = x - mean
            return amplitude * exp(dx*dx/-2.0/(sigma*sigma))
        }


        val kappa: Double by arguments
        var centerX: Optional<Int> by arguments
        var centerY: Optional<Int> by arguments
        val mode: String by arguments

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

            val totalMedian = matrix.fastMedian()
            val totalStddev = matrix.stddev()
            val low = totalMedian - totalStddev * kappa
            val high = totalMedian + totalStddev * kappa

            if (verboseMode) {
                println("Median value: $totalMedian")
                println("Standard deviation: $totalStddev")
                println("Sigma clipping range: $low .. $high")
            }

            if (!centerX.isPresent) {
                val csvWriter = if (verboseMode) {
                    val file = inputFile.prefixName("find_center_x_${channel}_").suffixExtension(".csv")
                    println("Saving $file")
                    val csvWriter = PrintWriter(FileWriter(file))
                    csvWriter.println("  Y, Amplitude, Mean, Sigma")
                    csvWriter
                } else {
                    null
                }
                val centerMeans = DoubleArray(inputImage.height)
                for (y in 0 until inputImage.height) {
                    val points = WeightedObservedPoints()
                    for (x in 0 until inputImage.width) {
                        val value = matrix.getPixel(x, y).toDouble()
                        if (value in low..high) {
                            points.add(x.toDouble(), value)
                        }
                    }
                    val gaussFit = GaussianCurveFitter.create().fit(points.toList())
                    csvWriter?.let {
                        it.println("  $y, ${gaussFit[0]}, ${gaussFit[1]}, ${gaussFit[2]}")
                    }
                    centerMeans[y] = gaussFit[1]
                }
                centerX = Optional.of((centerMeans.sigmaClip().median() + 0.5).toInt())
                println("Calculated centerX = ${centerX.get()}")
                csvWriter?.let {
                    it.close()
                }
            }

            if (!centerY.isPresent) {
                val csvWriter = if (verboseMode) {
                    val file = inputFile.prefixName("find_center_y_${channel}_").suffixExtension(".csv")
                    println("Saving $file")
                    val csvWriter = PrintWriter(FileWriter(file))
                    csvWriter.println("  X, Amplitude, Mean, Sigma")
                    csvWriter
                } else {
                    null
                }
                val centerMeans = DoubleArray(inputImage.width)
                for (x in 0 until inputImage.width) {
                    val points = WeightedObservedPoints()
                    for (y in 0 until inputImage.height) {
                        val value = matrix.getPixel(x, y).toDouble()
                        if (value in low..high) {
                            points.add(y.toDouble(), value)
                        }
                    }
                    val gaussFit = GaussianCurveFitter.create().fit(points.toList())
                    csvWriter?.let {
                        it.println("  $x, ${gaussFit[0]}, ${gaussFit[1]}, ${gaussFit[2]}")
                    }
                    centerMeans[x] = gaussFit[1]
                }
                centerY = Optional.of((centerMeans.sigmaClip().median() + 0.5).toInt())
                println("Calculated centerY = ${centerY.get()}")
                csvWriter?.let {
                    it.close()
                }
            }

            val calculatedMaxDistance = centerX.get() + centerY.get() // TODO calculate better
            val distanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }
            val clippedDistanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }

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

            // using apache
            val points = WeightedObservedPoints()
            for (i in xValues.indices) {
                points.add(xValues[i], yValues[i])
            }
            val polynomialFit2 = PolynomialCurveFitter.create(2).fit(points.toList())
            println("Apache polynomialFit2: ${polynomialFit2.contentToString()}")
            val gaussFit = GaussianCurveFitter.create().fit(points.toList())
            println("Apache gaussFit: ${gaussFit.contentToString()}")

            var error = 0.0
            for (i in xValues.indices) {
                val y1 = yValues[i]
                val y2 = polynomialFunction(xValues[i], polynomialFit2)
                val delta = y1 - y2
                error += delta * delta
            }
            error /= xValues.size
            println("Standard Error: $error")
            println()

            if (debugMode) {
                val file = inputFile.prefixName("vignette_curve_fit_${channel}_").suffixExtension(".csv")
                println("Saving $file")
                val csvWriter = PrintWriter(FileWriter(file))
                csvWriter.println("  Index, Count, Average, Median, Polynomial2, Gauss")
                for (i in 0 until maxDistance) {
                    val count = distanceValues[i].size
                    val average = distanceValues[i].toFloatArray().average().finiteOrElse()
                    val median = distanceValues[i].toFloatArray().medianInplace().finiteOrElse()
                    val polynomial2 = polynomialFunction(i.toDouble(), polynomialFit2)
                    val gauss = gaussFunction(i.toDouble(), gaussFit[0], gaussFit[1], gaussFit[2])
                    csvWriter.println("  $i, $count, $average, $median, $polynomial2, $gauss")
                }
                csvWriter.close()
                println()
            }

            val flatMatrix = CalculatedMatrix(inputImage.width, inputImage.height) { row, column ->
                val dx = (centerX.get() - column).toDouble()
                val dy = (centerY.get() - row).toDouble()
                val distance = sqrt(dx*dx + dy*dy)
                polynomialFunction(distance, polynomialFit2)
            }

            val flatMax = flatMatrix.max()
            val normalizedFlatMatrix = flatMatrix.copy().onEach { v -> v / flatMax }

            channelMatrices.add(normalizedFlatMatrix)
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
