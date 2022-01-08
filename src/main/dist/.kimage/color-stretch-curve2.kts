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
        list("curve") {
            description = """
                        The curve shape used to modify the contrast.
                        """
            point {
                min = Point(0.0, 0.0)
                max = Point(1.0, 1.0)
            }
            hint = Hint.Curve
            default = listOf(Point(0.0, 0.0), Point(0.4, 0.1), Point(0.6, 0.9), Point(1.0, 1.0))
        }
    }

    single {
        val brightness: Double by arguments
        val curve: List<Point> by arguments

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

            val histogramInputFile = inputFile.prefixName(outputDirectory, "hist_input_")
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

            val histogramBrightnessFile = inputFile.prefixName(outputDirectory, "hist_brightness_")
            println("Saving $histogramBrightnessFile (after brightness correction) for manual analysis")
            ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramBrightnessFile)
            println()
        }

        val curvePointsX = mutableListOf<Double>()
        val curvePointsY = mutableListOf<Double>()

        for (point in curve) {
            curvePointsX.add(point.x)
            curvePointsY.add(point.y)
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

            val histogramOutputFile = inputFile.prefixName(outputDirectory, "hist_output_")
            println("Saving $histogramOutputFile (after curve correction) for manual analysis")
            ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramOutputFile)
            println()
        }

        image
    }
}
