import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "resize"
    title = "Resize an image"
    description = """
                Resize an image according the specified arguments.
                """
    arguments {
        optionalDouble("factor") {
        }
        optionalDouble("factorX") {
        }
        optionalDouble("factorY") {
        }
        optionalInt("width") {
        }
        optionalInt("height") {
        }
        string("method") {
            allowed = listOf("nearest", "bilinear", "bicubic")
            default = "bicubic"
        }
    }

    single {
        var factor: Optional<Double> by arguments
        var factorX: Optional<Double> by arguments
        var factorY: Optional<Double> by arguments
        var width: Optional<Int> by arguments
        var height: Optional<Int> by arguments
        val method: String by arguments

        val scaling = when (method) {
            "nearest" -> Scaling.Nearest
            "bilinear" -> Scaling.Bilinear
            "bicubic" -> Scaling.Bicubic
            else -> throw IllegalArgumentException("Unknown scaling method: $method")
        }

        if (factor.isPresent) {
            println("Arguments:")
            println("  factor = ${factor.get()}")
            println("  method = $method")
            println()

            inputImage.scaleBy(factor.get(), factor.get(), scaling)
        } else if (factorX.isPresent || factorY.isPresent) {
            if (!factorX.isPresent) {
                factorX = Optional.of(1.0)
            }
            if (!factorY.isPresent) {
                factorY = Optional.of(1.0)
            }
            println("Arguments:")
            println("  factorX = ${factorX.get()}")
            println("  factorY = ${factorY.get()}")
            println("  method = $method")
            println()

            inputImage.scaleBy(factorX.get(), factorY.get(), scaling)
        } else {
            if (!width.isPresent) {
                if (height.isPresent) {
                    width = Optional.of(inputImage.width * height.get() / inputImage.height)
                }
            }
            if (!height.isPresent) {
                if (width.isPresent) {
                    height = Optional.of(inputImage.height * width.get() / inputImage.width)
                }
            }

            println("Arguments:")
            println("  width = ${width.get()}")
            println("  height = ${height.get()}")
            println("  method = $method")
            println()

            inputImage.scaleTo(width.get(), height.get(), scaling)
        }
    }
}
