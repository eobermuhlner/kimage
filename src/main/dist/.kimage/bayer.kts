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
    name = "bayer"
    title = "Bayer a color image"
    description = """
                Bayer a color image into a mosaic image (equivalent to a RAW image).
                """
    arguments {
        string("pattern") {
            description = """
                The color pattern of the Bayer 2x2 mosaic tile. 
                """
            allowed = listOf("rggb", "bggr", "gbrg", "grbg")
            default = "rggb"
        }
        optionalDouble("red") {
            description = """
                The factor used to multiply with the red values.
                """
        }
        optionalDouble("green") {
            description = """
                The factor used to multiply with the green values.
                """
        }
        optionalDouble("blue") {
            description = """
                The factor used to multiply with the blue values.
                """
        }
    }

    single {
        val pattern: String by arguments
        var red: Optional<Double> by arguments
        var green: Optional<Double> by arguments
        var blue: Optional<Double> by arguments

        if (!red.isPresent()) {
            red = Optional.of(1.0)
        }
        if (!green.isPresent()) {
            green = Optional.of(1.0)
        }
        if (!blue.isPresent()) {
            blue = Optional.of(1.0)
        }

        println("  red =   $red")
        println("  green = $green")
        println("  blue =  $blue")
        println()

        val width = inputImage.width
        val height = inputImage.height
        val mosaicMatrix = Matrix.matrixOf(width, height)

        val redMatrix = inputImage[Channel.Red]
        val greenMatrix = inputImage[Channel.Green]
        val blueMatrix = inputImage[Channel.Blue]

        val bayerMatrix = when (pattern) {
            "rggb" -> listOf(redMatrix, greenMatrix, greenMatrix, blueMatrix)
            "bggr" -> listOf(blueMatrix, greenMatrix, greenMatrix, redMatrix)
            "gbrg" -> listOf(greenMatrix, blueMatrix, greenMatrix, redMatrix)
            "grbg" -> listOf(greenMatrix, redMatrix, greenMatrix, blueMatrix)
            else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val bayerX = x % 2
                val bayerY = y % 2
                val bayerIndex = bayerX + bayerY * 2
                mosaicMatrix[x, y] = bayerMatrix[bayerIndex][x, y]
            }
        }

        MatrixImage(mosaicMatrix)
    }
}
