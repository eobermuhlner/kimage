import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "filter"
    title = "Filter an image"
    description = """
                Filter an image according the specified arguments.
                """
    arguments {
        string("filter") {
            description = """
                The filter algorithm.
                
                - `blur` uses a gaussian blur filter of the specified `radius`
                - `unsharpMask` uses an unsharp mask of the specified `radius`
                - `blur3x3` uses a gaussian blur filter using a 3x3 kernel
                - `blur5x5` uses a gaussian blur filter using a 5x5 kernel
                - `blur7x7` uses a gaussian blur filter using a 7x7 kernel
                - `median` uses a median filter of the specified `radius`
                - `average` uses an average filter of the specified `radius`
                - `sharpen3x3` uses a 3x3 sharpen mask
                - `unsharpMask3x3` uses a 3x3 unsharp mask
                - `edgeDetectionStrong3x3` detects edges along the horizontal/vertial axes and both diagonals using a 3x3 kernel
                - `edgeDetectionCross3x3` detects edges along the horizontal/vertial axes using a 3x3 kernel
                - `edgeDetectionDiagonal3x3` detects edges along both diagonals using a 3x3 kernel
                """
            allowed = listOf("blur", "median", "average", "unsharpMask", "sobel3x3", "sobel5x5", "blur3x3", "blur5x5", "blur7x7", "sharpen3x3", "unsharpMask3x3", "edgeDetectionStrong3x3", "edgeDetectionCross3x3", "edgeDetectionDiagonal3x3")
        }
        int("radius") {
            description = """
                The radius in pixels for the `blur`, 'unsharpMask', `median` and `average` filters.
                """
            enabledWhen = Reference("filter").isEqual("blur", "median", "average", "unsharpMask")
            min = 1
            default = 3
        }
        double("strength") {
            description = """
                The strength of the filter.
                """
            enabledWhen = Reference("filter").isEqual("unsharpMask")
            min = 0.0
            max = 1.0
            default = 0.5
        }
        string("channels") {
            allowed = listOf("rgb", "red", "green", "blue", "hue", "saturation", "brightness", "hsb", "hs", "hb", "sb")
            default = "rgb"
        }
    }

    single {
        val filter: String by arguments
        val radius: Int by arguments
        val strength: Double by arguments
        val channels: String by arguments

        fun matrixFilter(matrix: Matrix): Matrix {
            return when (filter) {
                "blur" -> matrix.gaussianBlurFilter(radius)
                "median" -> matrix.medianFilter(radius)
                "average" -> matrix.averageFilter(radius)
                "sharpen" -> matrix.sharpenFilter()
                "unsharpMask" -> matrix.unsharpMaskFilter(radius, strength)
                "unsharpMask3x3" -> matrix.convolute(KernelFilter.UnsharpMask)
                "blur3x3" -> matrix.convolute(KernelFilter.GaussianBlur3)
                "blur5x5" -> matrix.convolute(KernelFilter.GaussianBlur5)
                "blur7x7" -> matrix.convolute(KernelFilter.GaussianBlur7)
                "edgeDetectionStrong3x3" -> matrix.edgeDetectionStrongFilter()
                "edgeDetectionCross3x3" -> matrix.edgeDetectionCrossFilter()
                "edgeDetectionDiagonal3x3" -> matrix.edgeDetectionDiagonalFilter()
                "sobel3x3" -> matrix.sobelFilter3()
                "sobel5x5" -> matrix.sobelFilter5()
                else -> throw IllegalArgumentException("Unknown filter: $filter")
            }
        }

        when(channels) {
            "rgb", "red", "green", "blue" -> {
                var red = inputImage[Channel.Red]
                var green = inputImage[Channel.Green]
                var blue = inputImage[Channel.Blue]

                when(channels) {
                    "red" -> red = matrixFilter(red)
                    "green" -> green = matrixFilter(green)
                    "blue" -> blue = matrixFilter(blue)
                    "rgb" -> {
                        red = matrixFilter(red)
                        green = matrixFilter(green)
                        blue = matrixFilter(blue)
                    }
                    else -> throw IllegalArgumentException("Unknown channels: $channels")
                }

                MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to red,
                    Channel.Green to green,
                    Channel.Blue to blue)
            }
            else -> {
                var hue = inputImage[Channel.Hue]
                var saturation = inputImage[Channel.Saturation]
                var brightness = inputImage[Channel.Brightness]

                when(channels) {
                    "hue" -> hue = matrixFilter(hue)
                    "saturation" -> saturation = matrixFilter(saturation)
                    "brightness" -> brightness = matrixFilter(brightness)
                    "hsb" -> {
                        hue = matrixFilter(hue)
                        saturation = matrixFilter(saturation)
                        brightness = matrixFilter(brightness)
                    }
                    "hs" -> {
                        hue = matrixFilter(hue)
                        saturation = matrixFilter(saturation)
                    }
                    "hb" -> {
                        hue = matrixFilter(hue)
                        brightness = matrixFilter(brightness)
                    }
                    "sb" -> {
                        saturation = matrixFilter(saturation)
                        brightness = matrixFilter(brightness)
                    }
                    else -> throw IllegalArgumentException("Unknown channels: $channels")
                }

                val hsbImage = MatrixImage(inputImage.width, inputImage.height,
                    Channel.Hue to hue,
                    Channel.Saturation to saturation,
                    Channel.Brightness to brightness)

                MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to hsbImage[Channel.Red],
                    Channel.Green to hsbImage[Channel.Green],
                    Channel.Blue to hsbImage[Channel.Blue])
            }
        }
    }
}
