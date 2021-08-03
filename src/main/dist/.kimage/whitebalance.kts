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
                - `local` uses the median of a region centered at `localX`/`localY` with a radius of `localRadius` pixels.
                """
            allowed = listOf("custom", "global", "highlight", "local")
            default = "highlight"
        }
        optionalInt("localX") {
            description = """
                The center on the x axis of the local area to determine the gray value for white balancing.
                """
            hint = Hint.ImageX
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        optionalInt("localY") {
            description = """
                The center on the y axis of the local area to determine the gray value for white balancing.
                """
            hint = Hint.ImageY
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
        var localX: Optional<Int> by arguments
        var localY: Optional<Int> by arguments
        val localRadius: Int by arguments
        val highlight: Double by arguments
        val highlightChannel: String by arguments
        var red: Optional<Double> by arguments
        var green: Optional<Double> by arguments
        var blue: Optional<Double> by arguments

        if (!localX.isPresent) {
            localX = Optional.of(inputImage.width / 2)
        }
        if (!localY.isPresent) {
            localY = Optional.of(inputImage.height/ 2)
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
                val hightlightMatrix = inputImage[channel]
                val histogram = Histogram()
                histogram.add(hightlightMatrix)
                val highlightValue = histogram.estimatePercentile(highlight / 100.0)

                val redValues = mutableListOf<Double>()
                val greenValues = mutableListOf<Double>()
                val blueValues = mutableListOf<Double>()
                for (y in 0 until hightlightMatrix.height) {
                    for (x in 0 until hightlightMatrix.width) {
                        if (hightlightMatrix[x, y] >= highlightValue) {
                            redValues += redMatrix[x, y]
                            greenValues += greenMatrix[x, y]
                            blueValues += blueMatrix[x, y]
                        }
                    }
                }
                red = Optional.of(redValues.median())
                green = Optional.of(greenValues.median())
                blue = Optional.of(blueValues.median())
            }
            "local" -> {
                val halfLocalX = localX.get() / 2
                val halfLocalY = localY.get() / 2
                red = Optional.of(redMatrix.cropCenter(localRadius, halfLocalY, halfLocalX, false).median())
                green = Optional.of(greenMatrix.cropCenter(localRadius, halfLocalY, halfLocalX, false).median())
                blue = Optional.of(blueMatrix.cropCenter(localRadius, halfLocalY, halfLocalX, false).median())
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
        var redFactor = maxFactor / red.get()
        var greenFactor = maxFactor / green.get()
        var blueFactor = maxFactor / blue.get()

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
