import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.nio.file.*
import kotlin.math.*

kimage(0.1) {
    name = "hdr"
    title = "Stack multiple images with different exposures into a single HDR image"
    description = """
                Calculates for every pixel the values with the best exposure and merges them into a single HDR image.
                """
    arguments {
        int("saturationBlurRadius") {
            default = 3
        }
        double("contrastWeight") {
            default = 0.2
        }
        double("saturationWeight") {
            default = 0.1
        }
        double("exposureWeight") {
            default = 1.0
        }
    }

    multi {
        // based on: https://mericam.github.io/papers/exposure_fusion_reduced.pdf
        println("HDR stack multiple images")
        println()

        val saturationBlurRadius: Int by arguments
        val contrastWeight: Double by arguments
        val saturationWeight: Double by arguments
        val exposureWeight: Double by arguments

        println("Loading image: ${inputFiles[0]}")
        var baseImage: Image = ImageReader.read(inputFiles[0])
        val channels = baseImage.channels

        val weightChannelIndex = channels.size
        val hugeMatrixChannelCount = weightChannelIndex + 1

        val huge = HugeFloatArray(inputFiles.size, hugeMatrixChannelCount, baseImage.width, baseImage.height)

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

            val luminanceMatrix = image[Channel.Luminance]
            val saturationMatrix = image[Channel.Saturation].gaussianBlurFilter(saturationBlurRadius)
            val contrastMatrix = luminanceMatrix.convolute(KernelFilter.EdgeDetectionStrong)

            for (matrixIndex in 0 until luminanceMatrix.size) {
                val wellExposed = exp(-(luminanceMatrix[matrixIndex] - 0.5).pow(2)/0.08)
                val contrast = contrastMatrix[matrixIndex]
                val saturation = saturationMatrix[matrixIndex]
                val weight = contrast.pow(1.0) * contrastWeight +
                        saturation.pow(1.0) * saturationWeight +
                        wellExposed.pow(0.2) * exposureWeight
                huge[fileIndex, weightChannelIndex, matrixIndex] = weight.toFloat()
            }
        }
        println()

        val stackingMethod: (FloatArray, FloatArray) -> Float = { weightValues, values ->
            values.weightedAverage({ i, _ ->
                weightValues[i]
            })
        }

        println("Stacking ${inputFiles.size} images")
        val resultImage = MatrixImage(baseImage.width, baseImage.height, channels)
        val values = FloatArray(inputFiles.size)
        val weightValues = FloatArray(inputFiles.size)
        for (channelIndex in channels.indices) {
            val channel = channels[channelIndex]
            println("Stacking channel: $channel")
            val matrix = baseImage[channel]
            val resultMatrix = resultImage[channel]
            for (matrixIndex in 0 until matrix.size) {
                for (fileIndex in inputFiles.indices) {
                    values[fileIndex] = huge[fileIndex, channelIndex, matrixIndex]
                    weightValues[fileIndex] = huge[fileIndex, weightChannelIndex, matrixIndex]
                }

                val stackedValue = stackingMethod(weightValues, values)
                resultMatrix[matrixIndex] = stackedValue.toDouble()
            }
        }

        val outputFile = inputFiles[0].prefixName("hdr_")
        println("Saving $outputFile")
        ImageWriter.write(resultImage, outputFile)

        println()

        null
    }
}
