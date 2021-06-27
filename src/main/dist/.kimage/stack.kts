import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "stack"
    description = """
                Stacks multiple image using one of several algorithms.
                """
    arguments {
        string("method") {
            description = """
                        Method used to calculate the stacked image.
                        
                        The method `sigmaclip-median` removes outliers before using `median` on the remaining values.
                        The method `sigmaclip-average` removes outliers before using `average` on the remaining values.
                        """
            allowed = listOf("median", "average", "max", "min", "sigmaclip-median", "sigmaclip-average")
            default = "sigmaclip-median"
        }
        double("kappa") {
            description = """
                        The kappa factor is used in sigma-clipping to define how far from the center the outliers are allowed to be.
                        """
            min = 0.0
            default = 2.0
        }
        int("iterations") {
            description = """
                        The number of iterations used in sigma-clipping to remove outliers.
                        """
            min = 0
            default = 10
        }
    }

    multi {
        println("Stack multiple images")

        val method by arguments.string
        val kappa by arguments.double
        val iterations by arguments.int

        println("Arguments:")
        println("  method = $method")
        println("  kappa = $kappa")
        println("  iterations = $iterations")
        println()

        val stackingMethod: (FloatArray) -> Float = when(method) {
            "median" -> { array -> array.median() }
            "average" -> { array -> array.average() }
            "max" -> { array -> array.maxOrNull()!! }
            "min" -> { array -> array.minOrNull()!! }
            "sigmaclip-median" -> { array -> array.sigmaClip(kappa = kappa.toFloat(), iterations = iterations).median() }
            "sigmaclip-average" -> { array -> array.sigmaClip(kappa = kappa.toFloat(), iterations = iterations).average() }
            else -> throw IllegalArgumentException("Unknown method: " + method)
        }

        println("Loading image: ${inputFiles[0]}")
        var baseImage: Image = ImageReader.readMatrixImage(inputFiles[0])
        val channels = baseImage.channels
        val huge = HugeFloatArray(inputFiles.size, channels.size, baseImage.width, baseImage.height)

        for (fileIndex in inputFiles.indices) {
            val inputFile = inputFiles[fileIndex]

            val image = if (fileIndex == 0) {
                baseImage
            } else {
                println("Loading image: $inputFile")
                ImageReader.readMatrixImage(inputFile).crop(0, 0, baseImage.width, baseImage.height)
            }

            for (channelIndex in channels.indices) {
                val matrix = image[channels[channelIndex]]
                for (matrixIndex in 0 until matrix.size) {
                    huge[fileIndex, channelIndex, matrixIndex] = matrix[matrixIndex].toFloat()
                }
            }
        }
        println()

        println("Stacking ${inputFiles.size} images using $method")
        val resultImage = MatrixImage(baseImage.width, baseImage.height, channels)
        val values = FloatArray(inputFiles.size)
        for (channelIndex in channels.indices) {
            val channel = channels[channelIndex]
            print("Stacking channel: $channel")
            val matrix = baseImage[channel]
            val resultMatrix = resultImage[channel]
            for (matrixIndex in 0 until matrix.size) {
                for (fileIndex in inputFiles.indices) {
                    values[fileIndex] = huge[fileIndex, channelIndex, matrixIndex]
                }

                val stackedValue = stackingMethod(values)
                resultMatrix[matrixIndex] = stackedValue.toDouble()
            }
        }
        println()

        resultImage
    }
}
