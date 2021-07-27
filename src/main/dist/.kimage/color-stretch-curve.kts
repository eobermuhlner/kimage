import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "color-stretch-curve"
    title = "Stretch the colors non-linearly to fill the entire value range"
    description = """
                The colors are first brightened using a power function and then a curve is applied.
                
                The idea for this script is based on https://clarkvision.com/articles/astrophotography-rnc-color-stretch/
                """
    arguments {
        double("brightness") {
            description = """
                        The power value of the brightness increase.
                        
                        - A power value > 1 increases the brightness.
                        - A power value = 0 does not change the brightness.
                        - A power value < 1 increases the brightness.
                        """
            min = 0.0
            default = 2.0
        }
        string("curve") {
            description = """
                        The curve shape used to modify the contrast.
                        """
            allowed = listOf("linear", "s-curve", "s-curve-bright", "s-curve-dark", "s-curve-strong", "s-curve-super-strong", "bright+", "dark+", "bright-", "dark-", "custom1", "custom2", "all")
            default = "s-curve"
        }
        double("custom1X") {
            hint = Hint.ColorCurveX
            default = 0.3
        }
        double("custom1Y") {
            hint = Hint.ColorCurveY
            default = 0.01
        }
        double("custom2X") {
            hint = Hint.ColorCurveX
            default = 0.7
        }
        double("custom2Y") {
            hint = Hint.ColorCurveY
            default = 0.99
        }
    }

    single {
        val brightness: Double by arguments
        val curve: String by arguments
        val custom1X: Double by arguments
        val custom1Y: Double by arguments
        val custom2X: Double by arguments
        val custom2Y: Double by arguments

        val histogramWidth = 256
        val histogramHeight = 150

        val (power1, power2) = if (brightness < 1000.0) {
            Pair(brightness, 1.0)
        } else {
            Pair(brightness.pow(1.0 / 5.0), 5.0)
        }

        var image = inputImage

        if (debugMode) {
            println("Input image - average: ${image.values().average()}")
            println("Input image - median: ${image.values().fastMedian()}")
            println("Input image - stddev: ${image.values().stddev()}")

            val histogramInputFile = inputFile.prefixName("hist_input_")
            println("Saving $histogramInputFile for manual analysis")
            ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramInputFile)
            println()
        }

        if (power1 != 1.0) {
            image = image.onEach { v -> v.pow(1.0 / power1) }
        }
        if (power2 != 1.0) {
            image = image.onEach { v -> v.pow(1.0 / power2) }
        }

        if (debugMode) {
            println("After brightness correction - average: ${image.values().average()}")
            println("After brightness correction - median: ${image.values().fastMedian()}")
            println("After brightness correction - stddev: ${image.values().stddev()}")

            val histogramBrightnessFile = inputFile.prefixName("hist_brightness_")
            println("Saving $histogramBrightnessFile (after brightness correction) for manual analysis")
            ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramBrightnessFile)
            println()
        }


        val curves = if (curve == "all") {
            listOf("linear", "s-curve", "s-curve-bright", "s-curve-dark", "s-curve-strong", "s-curve-super-strong", "s-curve-extreme", "bright+", "dark+", "bright-", "dark-", "custom1", "custom2")
        } else {
            listOf(curve)
        }

        for (curve in curves) {
            val (curvePointsX, curvePointsY) = when(curve) {
                "linear" -> {
                    Pair(
                        listOf(0.0, 1.0),
                        listOf(0.0, 1.0)
                    )
                }
                "s-curve" -> {
                    Pair(
                        listOf(0.0, 0.3, 0.7, 1.0),
                        listOf(0.0, 0.2, 0.8, 1.0)
                    )
                }
                "s-curve-bright" -> {
                    Pair(
                        listOf(0.0, 0.2,  0.7, 1.0),
                        listOf(0.0, 0.18, 0.8, 1.0)
                    )
                }
                "s-curve-dark" -> {
                    Pair(
                        listOf(0.0, 0.3, 0.7, 1.0),
                        listOf(0.0, 0.2, 0.72, 1.0)
                    )
                }
                "s-curve-strong" -> {
                    Pair(
                        listOf(0.0, 0.2, 0.8, 1.0),
                        listOf(0.0, 0.1, 0.9, 1.0)
                    )
                }
                "s-curve-super-strong" -> {
                    Pair(
                        listOf(0.0, 0.2, 0.8, 1.0),
                        listOf(0.0, 0.05, 0.95, 1.0)
                    )
                }
                "s-curve-extreme" -> {
                    Pair(
                        listOf(0.0, 0.2, 0.8, 1.0),
                        listOf(0.0, 0.01, 0.99, 1.0)
                    )
                }
                "bright+" -> {
                    Pair(
                        listOf(0.0, 0.6, 1.0),
                        listOf(0.0, 0.7, 1.0)
                    )
                }
                "dark+" -> {
                    Pair(
                        listOf(0.0, 0.4, 1.0),
                        listOf(0.0, 0.5, 1.0)
                    )
                }
                "bright-" -> {
                    Pair(
                        listOf(0.0, 0.6, 1.0),
                        listOf(0.0, 0.5, 1.0)
                    )
                }
                "dark-" -> {
                    Pair(
                        listOf(0.0, 0.4, 1.0),
                        listOf(0.0, 0.3, 1.0)
                    )
                }
                "custom1" -> {
                    Pair(
                        listOf(0.0, custom1X, 1.0),
                        listOf(0.0, custom1Y, 1.0)
                    )
                }
                "custom2" -> {
                    Pair(
                        listOf(0.0, custom1X, custom2X, 1.0),
                        listOf(0.0, custom1Y, custom2Y, 1.0)
                    )
                }
                else -> throw IllegalArgumentException("Unknown curve: $curve")
            }

            println("Curve Points:")
            println("  X: $curvePointsX")
            println("  Y: $curvePointsY")
            println()

            val spline: SplineInterpolator = SplineInterpolator.createMonotoneCubicSpline(curvePointsX, curvePointsY)

            image = image.onEach { v -> spline.interpolate(v) }

            if (debugMode) {
                println("After curve correction - average: ${image.values().average()}")
                println("After curve correction - median: ${image.values().fastMedian()}")
                println("After curve correction - stddev: ${image.values().stddev()}")

                val histogramOutputFile = inputFile.prefixName("hist_output_")
                println("Saving $histogramOutputFile (after curve correction) for manual analysis")
                ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramOutputFile)
                println()
            }

            if (curve == "all") {
                val outputFile = inputFile.prefixName("color-stretch(${brightness},${curve})_")
                println("Saving $outputFile")
                ImageWriter.write(image, outputFile)
            }
        }

        image
    }
}
