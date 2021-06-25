import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

require(multiMode)

println("Stack multiple images")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

val methodName = parameters.getOrDefault("method", "sigmaclip-median")
val sigmaClipKappa = parameters.getOrDefault("kappa", "2.0").toFloat()
val sigmaClipIterations = parameters.getOrDefault("iterations", "5").toInt()

println("Parameters:")
println("  method = $methodName")
println("  kappa = $sigmaClipKappa")
println("  iterations = $sigmaClipIterations")
println()

val stackingMethod: (FloatArray) -> Float = when(methodName) {
    "median" -> { array -> array.median() }
    "average" -> { array -> array.average() }
    "max" -> { array -> array.maxOrNull()!! }
    "sigmaclip-median" -> { array -> array.sigmaClip(kappa = sigmaClipKappa, iterations = sigmaClipIterations).median() }
    "sigmaclip-average" -> { array -> array.sigmaClip(kappa = sigmaClipKappa, iterations = sigmaClipIterations).average() }
    else -> throw IllegalArgumentException("Unknown method: " + methodName)
}

println("Loading image: $files[0]")
var baseImage: Image = ImageReader.readMatrixImage(files[0])
val channels = baseImage.channels
val huge = HugeFloatArray(files.size, channels.size, baseImage.width, baseImage.height)

for (fileIndex in files.indices) {
    val inputFile = files[fileIndex]

    val image = if (fileIndex == 0) {
        baseImage
    } else {
        println("Loading image: $inputFile")
        ImageReader.readMatrixImage(inputFile)
    }

    for (channelIndex in channels.indices) {
        val matrix = image[channels[channelIndex]]
        for (matrixIndex in 0 until matrix.size) {
            huge[fileIndex, channelIndex, matrixIndex] = matrix[matrixIndex].toFloat()
        }
    }
}

val resultImage = MatrixImage(baseImage.width, baseImage.height, channels)
val values = FloatArray(files.size)
for (channelIndex in channels.indices) {
    val matrix = baseImage[channels[channelIndex]]
    val resultMatrix = resultImage[channels[channelIndex]]
    for (matrixIndex in 0 until matrix.size) {
        for (fileIndex in files.indices) {
            values[fileIndex] = huge[fileIndex, channelIndex, matrixIndex]
        }

        values.sort()
        val stackedValue = stackingMethod(values)
        resultMatrix[matrixIndex] = stackedValue.toDouble()
    }
}

resultImage
