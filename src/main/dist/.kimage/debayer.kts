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
        optionalFile("badpixels") {
            description = """
                The list of bad pixels (stuck, dead and hot pixels) that should be ignored during debayering.
                
                The badpixels file is a text file compatible with the format defined by `dcraw`.
                The format consists of space separated x and y coordinates per line.
                Everything after the second value is ignored.
                Lines starting with `#` will be ignored.
                
                Example:
                ```
                # This line will be ignored
                 345   807 this is a comment
                1447  2308
                ```
                """
            isFile = true
        }
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
        string("interpolation") {
            description = """
                The interpolation method used to debayer the mosaic image.
                
                - `superpixel` merges the 2x2 Bayer mosaic tile into a single pixel.
                  It has no practically artifacts and no chromatic errors.
                  The output image will have half the width and height of the input image.
                - `none` does not interpolate the 2x2 Bayer mosaic tile but simply colors each pixel with the appropriate color.
                  This is useful to visually analyze the mosaic image.
                - `nearest` is the simplest and fastest interpolation algorithm.
                  It has strong artifacts.
                - `bilinear` is a fast algorithm that bilinearly interpolates the neighboring pixels of each color.
                  It tends to smooth edges and create chromatic artifacts around them.
                """
            allowed = listOf("superpixel", "none", "nearest", "bilinear")
            default = "bilinear"
        }
    }

    single {
        val badpixels: Optional<File> by arguments
        val pattern: String by arguments
        var red: Optional<Double> by arguments
        var green: Optional<Double> by arguments
        var blue: Optional<Double> by arguments
        val interpolation: String by arguments

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

        val mosaicRedMatrix = DoubleMatrix(mosaic.rows / 2, mosaic.columns / 2)
        val mosaicGreen1Matrix = DoubleMatrix(mosaic.rows / 2, mosaic.columns / 2)
        val mosaicGreen2Matrix = DoubleMatrix(mosaic.rows / 2, mosaic.columns / 2)
        val mosaicBlueMatrix = DoubleMatrix(mosaic.rows / 2, mosaic.columns / 2)
        val mosaicGrayMatrix = DoubleMatrix(mosaic.rows / 2, mosaic.columns / 2)

        for (y in 0 until inputImage.height step 2) {
            for (x in 0 until inputImage.width step 2) {
                val r = mosaic.getPixel(x+rX, y+rY)
                val g1 = mosaic.getPixel(x+g1X, y+g1Y)
                val g2 = mosaic.getPixel(x+g2X, y+g2Y)
                val b = mosaic.getPixel(x+bX, y+bY)
                val gray = (r + r + g1 + g2 + b + b) / 6

                mosaicRedMatrix.setPixel(x/2, y/2, r)
                mosaicGreen1Matrix.setPixel(x/2, y/2, g1)
                mosaicGreen2Matrix.setPixel(x/2, y/2, g2)
                mosaicBlueMatrix.setPixel(x/2, y/2, b)
                mosaicGrayMatrix.setPixel(x/2, y/2, gray)
            }
        }

        if (red.isPresent) {
            red = Optional.of(1.0 / red.get())
        }
        if (green.isPresent) {
            green = Optional.of(1.0 / green.get())
        }
        if (blue.isPresent) {
            blue = Optional.of(1.0 / blue.get())
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

        println("  red =   $red")
        println("  green = $green")
        println("  blue =  $blue")
        println()

        val maxFactor = max(red.get(), max(green.get(), blue.get()))
        var redFactor = maxFactor / red.get()
        var greenFactor = maxFactor / green.get()
        var blueFactor = maxFactor / blue.get()

        println("Whitebalance:")
        println("  red =   $redFactor")
        println("  green = $greenFactor")
        println("  blue =  $blueFactor")

        var redOffset = 0.0
        var greenOffset = 0.0
        var blueOffset = 0.0

        mosaicRedMatrix.onEach { v -> (v - redOffset) * redFactor  }
        mosaicGreen1Matrix.onEach { v -> (v - greenOffset) * greenFactor  }
        mosaicGreen2Matrix.onEach { v -> (v - greenOffset) * greenFactor  }
        mosaicBlueMatrix.onEach { v -> (v - blueOffset) * blueFactor  }

        val redMatrix = Matrix.matrixOf(height, width)
        val greenMatrix = Matrix.matrixOf(height, width)
        val blueMatrix = Matrix.matrixOf(height, width)

        when (interpolation) {
            "superpixel" -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val r = mosaicRedMatrix.getPixel(x, y)
                        val g1 = mosaicGreen1Matrix.getPixel(x, y)
                        val g2 = mosaicGreen2Matrix.getPixel(x, y)
                        val b = mosaicBlueMatrix.getPixel(x, y)

                        redMatrix.setPixel(x, y, r)
                        greenMatrix.setPixel(x, y, (g1+g2)/2)
                        blueMatrix.setPixel(x, y, b)
                    }
                }
            }
            "none" -> {
                for (y in 0 until height step 2) {
                    for (x in 0 until width step 2) {
                        val r = mosaicRedMatrix.getPixel(x/2, y/2)
                        val g1 = mosaicGreen1Matrix.getPixel(x/2, y/2)
                        val g2 = mosaicGreen2Matrix.getPixel(x/2, y/2)
                        val b = mosaicBlueMatrix.getPixel(x/2, y/2)

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
                        val r = mosaicRedMatrix.getPixel(x/2, y/2)
                        val g1 = mosaicGreen1Matrix.getPixel(x/2, y/2)
                        val g2 = mosaicGreen2Matrix.getPixel(x/2, y/2)
                        val b = mosaicBlueMatrix.getPixel(x/2, y/2)

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
            "bilinear" -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val dx = x % 2
                        val dy = y % 2

                        val r: Double
                        val g: Double
                        val b: Double
                        if (dx == rX && dy == rY) {
                            r = mosaic.getPixel(x, y)
                            g = (mosaic.getPixel(x-1, y) + mosaic.getPixel(x+1, y) + mosaic.getPixel(x, y-1) + mosaic.getPixel(x, y+1)) / 4
                            b = (mosaic.getPixel(x-1, y-1) + mosaic.getPixel(x-1, y+1) + mosaic.getPixel(x+1, y-1) + mosaic.getPixel(x+1, y+1)) / 4
                        } else if (dx == bX && dy == bY) {
                            r = (mosaic.getPixel(x-1, y-1) + mosaic.getPixel(x-1, y+1) + mosaic.getPixel(x+1, y-1) + mosaic.getPixel(x+1, y+1)) / 4
                            g = (mosaic.getPixel(x-1, y) + mosaic.getPixel(x+1, y) + mosaic.getPixel(x, y-1) + mosaic.getPixel(x, y+1)) / 4
                            b = mosaic.getPixel(x, y)
                        } else {
                            g = mosaic.getPixel(x, y)
                            if ((x-1) % 2 == rX) {
                                r = (mosaic.getPixel(x-1, y) + mosaic.getPixel(x+1, y)) / 2
                                b = (mosaic.getPixel(x, y-1) + mosaic.getPixel(x, y+1)) / 2
                            } else {
                                r = (mosaic.getPixel(x, y-1) + mosaic.getPixel(x, y+1)) / 2
                                b = (mosaic.getPixel(x-1, y) + mosaic.getPixel(x+1, y)) / 2
                            }
                        }

                        redMatrix.setPixel(x, y, (r - redOffset) * redFactor)
                        greenMatrix.setPixel(x, y, (g - greenOffset) * greenFactor)
                        blueMatrix.setPixel(x, y, (b - blueOffset) * blueFactor)
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
