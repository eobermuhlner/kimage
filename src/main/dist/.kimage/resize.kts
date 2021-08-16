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
            description = """                
                The factor along both x and y axes by which the input image should be scaled.
                - Values > 1 enlarge the image
                - Values < 1 shrink the image
                
                If the axes should be scaled by different factors, use `factorX` and `factorY` instead.
                If the absolute target width and height are known, use `width` and `height` instead.
                """
        }
        optionalDouble("factorX") {
            description = """
                The factor along the x axis by which the input image should be scaled.
                - Values > 1 enlarge the image
                - Values < 1 shrink the image
                
                Usually used together with `factorY`.
                If both axes are scaled by the same factor, use `factor` instead.
                If the absolute target width is known, use `width` instead.
                """
        }
        optionalDouble("factorY") {
            description = """
                The factor along the y axis by which the input image should be scaled.
                - Values > 1 enlarge the image
                - Values < 1 shrink the image
                
                Usually used together with `factorY`.
                If both axes are scaled by the same factor, use `factor` instead.
                If the absolute target height is known, use `height` instead.
                """
        }
        optionalInt("width") {
            description = """
                The absolute target width of the output image.
                
                If you want to scale by a relative factor use `factorX` or `factor` instead. 
                """
        }
        optionalInt("height") {
            description = """
                The absolute target height of the output image.
                
                If you want to scale by a relative factor use `factorY` or `factor` instead. 
                """
        }
        double("offsetX") {
            description = """
                The offset on the x-axis in the input image.
                May be a sub-pixel value.
                """
            default = 0.0
        }
        double("offsetY") {
            description = """
                The offset on the y-axis in the input image.
                May be a sub-pixel value.
                """
            default = 0.0
        }
        string("method") {
            description = """
                The interpolation method used to scale pixels.
                
                - `nearest` is the fastest and simplest algorithm.
                - `bilinear` is a fast algorithm that tends to smooth edges.
                - `bicubic` is a slower algorithm that usually produces good results.
                """
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
        val offsetX: Double by arguments
        val offsetY: Double by arguments
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
            println("  offsetX = $offsetX")
            println("  offsetY = $offsetY")
            println()

            inputImage.scaleBy(factor.get(), factor.get(), offsetX, offsetY, scaling)
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
            println("  offsetX = $offsetX")
            println("  offsetY = $offsetY")
            println("  method = $method")
            println()

            inputImage.scaleBy(factorX.get(), factorY.get(), offsetX, offsetY, scaling)
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
            println("  offsetX = $offsetX")
            println("  offsetY = $offsetY")
            println("  method = $method")
            println()

            inputImage.scaleTo(width.get(), height.get(), offsetX, offsetY, scaling)
        }
    }
}
