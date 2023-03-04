import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "layer"
    title = "Merge multiple images"
    description = """
                Merge multiple images.
                The first image is the main image to merge, all other images are the mask to apply successively.
                This function is usually used with two input images.
                See: https://docs.gimp.org/2.8/en/gimp-concepts-layer-modes.html
                """
    arguments {
        string("operation") {
            description = """
                        The operation to use for merging.
                    """
            allowed = listOf("plus", "minus", "multiply", "divide", "min", "max", "screen", "avg", "overlay", "dodge", "burn", "hardlight", "softlight", "grainextract", "grainmerge", "difference")
            default = "screen"
        }
    }

    multi {
        val operation: String by arguments

        println("Loading base image ${inputFiles[0]}")
        var baseImage = ImageReader.read(inputFiles[0])
        println()

        for (i in 1 until inputFiles.size) {
            val inputFile = inputFiles[i]
            println("Loading image ${inputFile}")
            val image = ImageReader.read(inputFile).crop(0, 0, baseImage.width, baseImage.height, false)

            val func: (Double, Double) -> Double = when(operation) {
                "plus" -> { a, b -> a + b }
                "minus" -> { a, b -> a - b }
                "multiply" -> { a, b -> a * b }
                "divide" -> { a, b -> if (b == 0.0) 1.0 else a / b }
                "min" -> { a, b -> min(a, b) }
                "max" -> { a, b -> max(a, b) }
                "screen" -> { a, b -> 1.0 - (1.0 - a) * (1.0 - b) }
                "avg" -> { a, b -> (a + b) / 2.0 }
                "overlay" -> { a, b -> a * (a + 2*b + (1.0 - a)) }
                "dodge" -> { a, b ->  if (b == 0.0) 1.0 else a / (1.0 - b) }
                "burn" -> { a, b ->  if (b == 0.0) 1.0 else 1.0 - (1.0 - a) / b }
                "hardlight" -> { a, b ->  if (b > 0.5) (1.0 - (1.0 - 2*(b-0.5)) * (1.0-a)) else (2*a*b) }
                "softlight" -> { a, b ->
                    val r = 1.0 - (1.0 - a) * (1.0 - b)
                    ((1.0 - a) * b + r) * a
                }
                "grainextract" -> { a, b -> a - b + 0.5 }
                "grainmerge" -> { a, b -> a + b - 0.5 }
                "difference" -> { a, b -> abs(a - b) }
                else -> throw IllegalArgumentException("Unknown operation: $operation")
            }


            baseImage = MatrixImage(baseImage.width, baseImage.height, baseImage.channels) { channel, width, height ->
                DoubleMatrix(width, height) { x, y -> clamp(func(baseImage[channel][x, y], image[channel][x, y]), 0.0, 1.0) }
            }
        }

        baseImage
    }
}
