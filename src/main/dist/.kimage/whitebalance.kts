import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

kimage(0.1) {
    name = "whitebalance"
    title = "Correct the whitebalance of an image"
    description = """
                Correct the whitebalance of an image.
                """
    arguments {
        string ("whitebalance") {
            description = """
                The whitebalancing algorithm.
                
                - `custom` specifies the concrete multipliers for `red`, `green` and `blue` channel.
                - `global` uses the median of the entire input image to determine the gray value.
                - `highlight` uses the median of the highlighted pixels of the entire input image to determine the gray value.
                   Use `highlight` to specify the percentile of the pixels that should be used
                - `local` uses the median of a region centered at the point `localCenter` with a radius of `localRadius` pixels.
                """
            allowed = listOf("custom", "global", "highlight", "local")
            default = "highlight"
        }
        optionalPoint("localCenter") {
            description = """
                The center of the local area to determine the gray value for white balancing.
                """
            hint = Hint.ImageXY
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        int("localRadius") {
            description = """
                The radius of the local area to determine the gray value for white balancing.
                """
            enabledWhen = Reference("whitebalance").isEqual("local")
            default = 10
        }
        double("highlight") {
            description = """
                The percentile of the hightlights to determine the gray value for white balancing.
                """
            unit = "% percentile"
            enabledWhen = Reference("whitebalance").isEqual("highlight")
            default = 80.0
        }
        string("highlightChannel") {
            description = """
                The channel to measure the highlights to determine the gray value for white balancing.
                """
            enabledWhen = Reference("whitebalance").isEqual("highlight")
            allowed = listOf("red", "green", "blue", "gray", "luminance")
            default = "gray"
        }
        boolean("ignoreOverExposed") {
            description = """
                        Ignore pixels where at least one color channel is at maximum level.
                        This will ignore overexposed pixels.
                        """
            enabledWhen = Reference("whitebalance").isEqual("highlight")
            default = true
        }
        optionalDouble("red") {
            description = """
                The red value for custom white balancing.
                """
            enabledWhen = Reference("whitebalance").isEqual("custom")
        }
        optionalDouble("green") {
            description = """
                The green value for custom white balancing.
                """
            enabledWhen = Reference("whitebalance").isEqual("custom")
        }
        optionalDouble("blue") {
            description = """
                The blue  value for custom white balancing.
                """
            enabledWhen = Reference("whitebalance").isEqual("custom")
        }
    }

    single {
        var whitebalance: String by arguments
        var localCenter: Optional<Point> by arguments
        val localRadius: Int by arguments
        val highlight: Double by arguments
        val highlightChannel: String by arguments
        val ignoreOverExposed: Boolean by arguments
        var red: Optional<Double> by arguments
        var green: Optional<Double> by arguments
        var blue: Optional<Double> by arguments

        if (!localCenter.isPresent) {
            localCenter = Optional.of(Point(inputImage.width / 2, inputImage.height/ 2))
        }

        val redMatrix = inputImage[Channel.Red]
        val greenMatrix = inputImage[Channel.Green]
        val blueMatrix = inputImage[Channel.Blue]

        when (whitebalance) {
            "custom" -> {
                if (red.isPresent) {
                    red = Optional.of(1.0 / red.get())
                }
                if (green.isPresent) {
                    green = Optional.of(1.0 / green.get())
                }
                if (blue.isPresent) {
                    blue = Optional.of(1.0 / blue.get())
                }
            }
            "global" -> {
                red = Optional.of(redMatrix.median())
                green = Optional.of(greenMatrix.median())
                blue = Optional.of(blueMatrix.median())
            }
            "highlight" -> {
                val channel = when (highlightChannel) {
                    "red" -> Channel.Red
                    "green" -> Channel.Green
                    "blue" -> Channel.Blue
                    "gray" -> Channel.Gray
                    "luminance" -> Channel.Luminance
                    else -> throw IllegalArgumentException("Unknown channel: $highlightChannel")
                }
                val highlightMatrix = inputImage[channel]
                val histogram = Histogram()
                histogram.add(highlightMatrix)
                val highlightValue = histogram.estimatePercentile(highlight / 100.0)

                var overExposedCount = 0
                val redValues = mutableListOf<Double>()
                val greenValues = mutableListOf<Double>()
                val blueValues = mutableListOf<Double>()
                for (y in 0 until highlightMatrix.height) {
                    for (x in 0 until highlightMatrix.width) {
                        if (highlightMatrix[x, y] >= highlightValue) {
                            val r = redMatrix[x, y]
                            val g = greenMatrix[x, y]
                            val b = blueMatrix[x, y]
                            if (ignoreOverExposed && (r >= 1.0 || g >= 1.0 || b >= 1.0)) {
                                overExposedCount++
                            } else {
                                redValues += r
                                greenValues += g
                                blueValues += b
                            }
                        }
                    }
                }
                if (verboseMode && ignoreOverExposed) {
                    println("Over exposure: $overExposedCount pixels ignored")
                }
                if (verboseMode) {
                    println("Highlight ${highlight} (>= $highlightValue in $channel): ${redValues.size} pixels found")
                }
                red = Optional.of(redValues.median())
                green = Optional.of(greenValues.median())
                blue = Optional.of(blueValues.median())
            }
            "local" -> {
                val centerX = localCenter.get().x.toInt()
                val centerY = localCenter.get().y.toInt()
                red = Optional.of(redMatrix.cropCenter(localRadius, centerX, centerY, false).median())
                green = Optional.of(greenMatrix.cropCenter(localRadius, centerX, centerY, false).median())
                blue = Optional.of(blueMatrix.cropCenter(localRadius, centerX, centerY, false).median())
            }
            else -> throw IllegalArgumentException("Unknown whitebalance: $whitebalance")
        }

        if (!red.isPresent()) {
            red = Optional.of(1.0)
        }
        if (!green.isPresent()) {
            green = Optional.of(1.0)
        }
        if (!blue.isPresent()) {
            blue = Optional.of(1.0)
        }

        val maxFactor = max(red.get(), max(green.get(), blue.get()))
        var redFactor = if (red.get() == 0.0) 1.0 else maxFactor / red.get()
        var greenFactor = if (green.get() == 0.0) 1.0 else maxFactor / green.get()
        var blueFactor = if (blue.get() == 0.0) 1.0 else maxFactor / blue.get()

        println("Whitebalance Factor:")
        println("  red =   $redFactor")
        println("  green = $greenFactor")
        println("  blue =  $blueFactor")
        println()

        var redOffset = 0.0
        var greenOffset = 0.0
        var blueOffset = 0.0

        redMatrix.onEach { v -> (v - redOffset) * redFactor  }
        greenMatrix.onEach { v -> (v - greenOffset) * greenFactor  }
        blueMatrix.onEach { v -> (v - blueOffset) * blueFactor  }

        MatrixImage(inputImage.width, inputImage.height,
            Channel.Red to redMatrix,
            Channel.Green to greenMatrix,
            Channel.Blue to blueMatrix)
    }
}
