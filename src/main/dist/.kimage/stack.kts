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
    title = "Stack multiple image using one of several algorithms"
    description = """
                After loading all images one of the following stacking methods is applied on the RGB channels:
                
                - `median` takes the median values of every pixel of each input image.
                - `average` takes the average values of every pixel of each input image.
                - `max` takes the maximum value of every pixel of each input image.
                - `min` takes the minimum value of every pixel of each input image.
                - `sigma-clip-median` removes outliers before using `median` on the remaining values.
                - `sigma-clip-average` removes outliers before using `average` on the remaining values.
                - `sigma-winsorize-median` replaces outliers with the nearest value in sigma range before using `median`.
                - `sigma-winsorize-average` replaces outliers with the nearest value in sigma range before using `average`.
                - `winsorized-sigma-clip-median` replaces outliers with the nearest value in sigma range before sigma-clipping and then using `median`.
                - `winsorized-sigma-clip-average` replaces outliers with the nearest value in sigma range before sigma-clipping and then using `average`.
                - `all` runs all of the available methods and produces an output image for each method.
                        
                All methods that use sigma-clipping print a histogram with the information how many input values where actually used to stack each output value. 
                """
    arguments {
        string("method") {
            description = """
                        Method used to calculate the stacked image.                        
                        """
            allowed = listOf("median", "average", "max", "min", "sigma-clip-median", "sigma-clip-average", "sigma-winsorize-median", "sigma-winsorize-average", "winsorized-sigma-clip-median", "winsorized-sigma-clip-average", "all")
            default = "sigma-clip-median"
        }
        double("kappa") {
            description = """
                        The kappa factor is used in sigma-clipping to define how far from the center the outliers are allowed to be.
                        """
            enabledWhen = Reference("method").isEqual("sigma-clip-median", "sigma-clip-average", "sigma-winsorize-median", "sigma-winsorize-average", "winsorized-sigma-clip-median", "winsorized-sigma-clip-average", "all")
            min = 0.0
            default = 2.0
        }
        int("iterations") {
            description = """
                        The number of iterations used in sigma-clipping to remove outliers.
                        """
            enabledWhen = Reference("method").isEqual("sigma-clip-median", "sigma-clip-average", "winsorized-sigma-clip-median", "winsorized-sigma-clip-average", "all")
            min = 0
            default = 10
        }
    }

    multi {
        val method: String by arguments
        val kappa: Double by arguments
        val iterations: Int by arguments

        println("Loading image: ${inputFiles[0]}")
        var baseImage: Image = ImageReader.read(inputFiles[0])
        val channels = baseImage.channels
        val huge = HugeFloatArray(inputFiles.size, channels.size, baseImage.width, baseImage.height)

        for (fileIndex in inputFiles.indices) {
            val inputFile = inputFiles[fileIndex]

            val image = if (fileIndex == 0) {
                baseImage
            } else {
                println("Loading image: $inputFile")
                ImageReader.read(inputFile).crop(0, 0, baseImage.width, baseImage.height)
            }

            for (channelIndex in channels.indices) {
                val matrix = image[channels[channelIndex]]
                for (matrixIndex in 0 until matrix.size) {
                    huge[fileIndex, channelIndex, matrixIndex] = matrix[matrixIndex].toFloat()
                }
            }
        }
        println()

        val methods = if (method == "all") {
            listOf("median", "average", "max", "min", "sigma-clip-median", "sigma-clip-average", "sigma-winsorize-median", "sigma-winsorize-average", "winsorized-sigma-clip-median", "winsorized-sigma-clip-average")
        } else {
            listOf(method)
        }

        for (method in methods) {
            val sigmaClipHistogram = Histogram(inputFiles.size + 1)

            val stackingMethod: (FloatArray) -> Float = when(method) {
                "median" -> { array -> array.median() }
                "average" -> { array -> array.average() }
                "max" -> { array -> array.maxOrNull()!! }
                "min" -> { array -> array.minOrNull()!! }
                "sigma-clip-median" -> { array ->
                    val clippedLength = array.sigmaClipInplace(kappa.toFloat(), iterations, histogram = sigmaClipHistogram)
                    array.medianInplace(0, clippedLength)
                }
                "sigma-clip-average" -> { array ->
                    val clippedLength = array.sigmaClipInplace(kappa.toFloat(), iterations, histogram = sigmaClipHistogram
                    )
                    array.average(0, clippedLength)
                }
                "sigma-winsorize-median" -> { array ->
                    array.sigmaWinsorizeInplace(kappa.toFloat())
                    array.medianInplace()
                }
                "sigma-winsorize-average" -> { array ->
                    array.sigmaWinsorizeInplace(kappa.toFloat())
                    array.average()
                }
                "winsorized-sigma-clip-median" -> { array ->
                    val clippedLength = array.huberWinsorizedSigmaClipInplace(kappa = kappa.toFloat(), iterations, histogram = sigmaClipHistogram)
                    array.medianInplace(0, clippedLength)
                }
                "winsorized-sigma-clip-average" -> { array ->
                    val clippedLength = array.huberWinsorizedSigmaClipInplace(kappa = kappa.toFloat(), iterations, histogram = sigmaClipHistogram)
                    array.average(0, clippedLength)
                }
                else -> throw IllegalArgumentException("Unknown method: " + method)
            }

            println("Stacking ${inputFiles.size} images using $method")
            val resultImage = MatrixImage(baseImage.width, baseImage.height, channels)
            val values = FloatArray(inputFiles.size)
            for (channelIndex in channels.indices) {
                val channel = channels[channelIndex]
                println("Stacking channel: $channel")
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

            if (sigmaClipHistogram.n > 0) {
                println("Sigma-Clip Histogram")
                sigmaClipHistogram.print()
                println()
            }

            val outputFile = inputFiles[0].prefixName(outputDirectory, "stack(${method})_")
            println("Saving $outputFile")
            ImageWriter.write(resultImage, outputFile)

            println()
        }

        null
    }
}
