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
            allowed = listOf("custom", "global", "highlight", "local")
            default = "custom"
        }
        optionalInt("localX") {
            hint = Hint.ImageX
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        optionalInt("localY") {
            hint = Hint.ImageY
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        int("localRadius") {
            default = 10
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        double("highlight") {
            default = 0.8
            enabledWhen = Reference("whitebalance").isEqual("highlight")
        }
        optionalDouble("red") {
            enabledWhen = Reference("whitebalance").isEqual("custom")
        }
        optionalDouble("green") {
            enabledWhen = Reference("whitebalance").isEqual("custom")
        }
        optionalDouble("blue") {
            enabledWhen = Reference("whitebalance").isEqual("custom")
        }
    }

    single {
        var whitebalance: String by arguments
        var localX: Optional<Int> by arguments
        var localY: Optional<Int> by arguments
        val localRadius: Int by arguments
        val highlight: Double by arguments
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
                val grayMatrix = inputImage[Channel.Gray]
                val histogram = Histogram()
                histogram.add(grayMatrix)
                val highlightValue = histogram.estimatePercentile(highlight)

                val redValues = mutableListOf<Double>()
                val greenValues = mutableListOf<Double>()
                val blueValues = mutableListOf<Double>()
                for (row in 0 until grayMatrix.rows) {
                    for (column in 0 until grayMatrix.columns) {
                        if (grayMatrix[row, column] >= highlightValue) {
                            redValues += redMatrix[row, column]
                            greenValues += greenMatrix[row, column]
                            blueValues += blueMatrix[row, column]
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
