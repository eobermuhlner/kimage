import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "color-stretch"
    description = """
                Stretches the colors of an image to fill the entire value range.
                """
    arguments {
        double("brightness") {
            description = """
                        The power value of the brightness increase.
                        
                        A power value > 1 increases the brightness.
                        A power value = 0 does not change the brightness.
                        A power value < 1 increases the brightness.
                        """
            min = 0.0
            default = 2.0
        }
        string("curve") {
            description = """
                        The curve shape used to modify the contrast.
                        """
            allowed = listOf("linear", "s-curve", "s-curve-bright", "s-curve-dark", "bright+", "dark+", "bright-", "dark-", "custom1", "custom2")
            default = "s-curve"
        }
        double("custom1X") {
            default = 0.2
        }
        double("custom1Y") {
            default = 0.1
        }
        double("custom2X") {
            default = 0.8
        }
        double("custom2Y") {
            default = 0.9
        }
    }

    single {
        println("Color stretching")
        println()

        val brightness: Double by arguments
        val curve: String by arguments
        val custom1X: Double by arguments
        val custom1Y: Double by arguments
        val custom2X: Double by arguments
        val custom2Y: Double by arguments

        println("Arguments:")
        println("  brightness = $brightness")
        println("  curve = $curve")
        when (curve) {
            "custom1" -> {
                println("  custom1X = $custom1X")
                println("  custom1Y = $custom1Y")
            }
            "custom2" -> {
                println("  custom1X = $custom1X")
                println("  custom1Y = $custom1Y")
                println("  custom2X = $custom2X")
                println("  custom2Y = $custom2Y")
            }
        }
        println()

        val (power1, power2) = if (brightness < 1000.0) {
            Pair(brightness, 1.0)
        } else {
            Pair(brightness.pow(1.0 / 5.0), 5.0)
        }

        var image = inputImage
        if (power1 != 1.0) {
            image = image.onEach { v -> v.pow(1.0 / power1) }
        }
        if (power2 != 1.0) {
            image = image.onEach { v -> v.pow(1.0 / power2) }
        }

        val spline: SplineInterpolator = when(curve) {
            "linear" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 1.0),
                    listOf(0.0, 1.0)
                )
            }
            "s-curve" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.3, 0.7, 1.0),
                    listOf(0.0, 0.2, 0.8, 1.0)
                )
            }
            "s-curve-bright" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.2,  0.7, 1.0),
                    listOf(0.0, 0.18, 0.8, 1.0)
                )
            }
            "s-curve-dark" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.3, 0.7, 1.0),
                    listOf(0.0, 0.2, 0.72, 1.0)
                )
            }
            "bright+" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.6, 1.0),
                    listOf(0.0, 0.7, 1.0)
                )
            }
            "dark+" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.4, 1.0),
                    listOf(0.0, 0.5, 1.0)
                )
            }
            "bright-" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.6, 1.0),
                    listOf(0.0, 0.5, 1.0)
                )
            }
            "dark-" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, 0.4, 1.0),
                    listOf(0.0, 0.3, 1.0)
                )
            }
            "custom1" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, custom1X, 1.0),
                    listOf(0.0, custom1Y, 1.0)
                )
            }
            "custom2" -> {
                SplineInterpolator.createMonotoneCubicSpline(
                    listOf(0.0, custom1X, custom2X, 1.0),
                    listOf(0.0, custom1X, custom2Y, 1.0)
                )
            }
            else -> throw IllegalArgumentException("Unknown curve: $curve")
        }

        image = image.onEach { v -> spline.interpolate(v) }

        image
    }
}
