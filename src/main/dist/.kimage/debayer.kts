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
    name = "debayer"
    title = "Debayer a raw image into a color image"
    description = """
                Debayer the mosaic of a raw image into a color image.
                """
    arguments {
        string("pattern") {
            allowed = listOf("rggb", "bggr", "gbrg", "grbg")
            default = "rggb"
        }
        double("red") {
            default = 1.0
        }
        double("green") {
            default = 1.0
        }
        double("blue") {
            default = 1.0
        }
        string("interpolation") {
            allowed = listOf("superpixel", "none", "nearest", "bilinear")
            default = "superpixel"
        }
        optionalFile("badpixels") {
            isFile = true
        }
    }

    single {
        val interpolation: String by arguments
        val pattern: String by arguments
        val red: Double by arguments
        val green: Double by arguments
        val blue: Double by arguments
        val badpixels: Optional<File> by arguments

        val badpixelCoords = if (badpixels.isPresent()) {
            badpixels.get().readLines()
                .filter { !it.isBlank() }
                .filter { !it.startsWith("#") }
                .map {
                    val values = it.trim().split(Regex("\\s+"))
                    if (values.size >= 2) {
                        Pair(Integer.parseInt(values[0]), Integer.parseInt(values[1]))
                    } else {
                        throw java.lang.IllegalArgumentException("Format must be 'x y'")
                    }
                }.toSet()
        } else {
            setOf()
        }

        val (width, height) = when (interpolation) {
            "superpixel" -> Pair(inputImage.width / 2, inputImage.height / 2)
            else -> Pair(inputImage.width, inputImage.height)
        }

        val (rX, rY) = when (pattern) {
            "rggb" -> Pair(0, 0)
            "bggr" -> Pair(1, 1)
            "gbrg" -> Pair(0, 1)
            "grbg" -> Pair(0, 1)
            else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
        }
        val (g1X, g1Y) = when (pattern) {
            "rggb" -> Pair(0, 1)
            "bggr" -> Pair(0, 1)
            "gbrg" -> Pair(0, 0)
            "grbg" -> Pair(0, 0)
            else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
        }
        val (g2X, g2Y) = when (pattern) {
            "rggb" -> Pair(1, 0)
            "bggr" -> Pair(1, 0)
            "gbrg" -> Pair(1, 1)
            "grbg" -> Pair(1, 1)
            else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
        }
        val (bX, bY) = when (pattern) {
            "rggb" -> Pair(1, 1)
            "bggr" -> Pair(0, 0)
            "gbrg" -> Pair(0, 1)
            "grbg" -> Pair(1, 0)
            else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
        }

        val mosaic = inputImage[Channel.Gray]

        println("Bad pixels: $badpixelCoords")
        for (badpixelCoord in badpixelCoords) {
            val x = badpixelCoord.first
            val y = badpixelCoord.second

            val surroundingValues = mutableListOf<Double>()
            for (dy in -2 .. +2 step 4) {
                for (dx in -2 .. +2 step 4) {
                    if (mosaic.isPixelInside(x + dx, y + dy) && !badpixelCoords.contains(Pair(x + dx, y + dy))) {
                        surroundingValues.add(mosaic.getPixel(x + dx, y + dy))
                    }
                }
            }

            mosaic.setPixel(x, y, surroundingValues.median())
        }


        val redMatrix = Matrix.matrixOf(height, width)
        val greenMatrix = Matrix.matrixOf(height, width)
        val blueMatrix = Matrix.matrixOf(height, width)

        when (interpolation) {
            "superpixel" -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val r = mosaic.getPixel(x*2+rX, y*2+rY) * red
                        val g1 = mosaic.getPixel(x*2+g1X, y*2+g1Y) * green
                        val g2 = mosaic.getPixel(x*2+g2X, y*2+g2Y) * green
                        val b = mosaic.getPixel(x*2+bX, y*2+bY) * blue
                        redMatrix.setPixel(x, y, r)
                        greenMatrix.setPixel(x, y, (g1+g2)/2)
                        blueMatrix.setPixel(x, y, b)
                    }
                }
            }
            "none" -> {
                for (y in 0 until height step 2) {
                    for (x in 0 until width step 2) {
                        val r = mosaic.getPixel(x+rX, y+rY) * red
                        val g1 = mosaic.getPixel(x+g1X, y+g1Y) * green
                        val g2 = mosaic.getPixel(x+g2X, y+g2Y) * green
                        val b = mosaic.getPixel(x+bX, y+bY) * blue
                        redMatrix.setPixel(x+rX, y+rY, r)
                        greenMatrix.setPixel(x+g1X, y+g1Y, g1)
                        greenMatrix.setPixel(x+g2X, y+g2Y, g2)
                        blueMatrix.setPixel(x+bX, y+bY, b)
                    }
                }
            }
            "nearest" -> {
                for (y in 0 until height step 2) {
                    for (x in 0 until width step 2) {
                        val r = mosaic.getPixel(x+rX, y+rY) * red
                        val g1 = mosaic.getPixel(x+g1X, y+g1Y) * green
                        val g2 = mosaic.getPixel(x+g2X, y+g2Y) * green
                        val b = mosaic.getPixel(x+bX, y+bY) * blue
                        redMatrix.setPixel(x+0, y+0, r)
                        redMatrix.setPixel(x+1, y+0, r)
                        redMatrix.setPixel(x+0, y+1, r)
                        redMatrix.setPixel(x+1, y+1, r)
                        blueMatrix.setPixel(x+0, y+0, b)
                        blueMatrix.setPixel(x+1, y+0, b)
                        blueMatrix.setPixel(x+0, y+1, b)
                        blueMatrix.setPixel(x+1, y+1, b)
                        greenMatrix.setPixel(x+0, y+0, g1)
                        greenMatrix.setPixel(x+1, y+0, g1)
                        greenMatrix.setPixel(x+0, y+1, g2)
                        greenMatrix.setPixel(x+1, y+1, g2)
                    }
                }
            }
            else -> throw java.lang.IllegalArgumentException("Unknown interpolation: $interpolation")
        }

        MatrixImage(width, height,
            Channel.Red to redMatrix,
            Channel.Green to greenMatrix,
            Channel.Blue to blueMatrix)
    }
}
