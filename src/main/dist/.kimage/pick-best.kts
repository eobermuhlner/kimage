import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "pick-best"
    title = "Picks the best images"
    description = """
                Picks the best images.
                
                The input images must already be aligned.
                """
    arguments {
        int("centerX") {
            description = """
                        The center x coordinate to measure for similarity.
                        """
            hint = Hint.ImageX
        }
        int("centerY") {
            description = """
                        The center y coordinate to measure for similarity.
                        """
            hint = Hint.ImageY
        }
        int("radius") {
            description = """
                        The radius to measure for similarity.
                        """
            min = 1
        }
        string("prefix") {
            description = """
                        The prefix for the copied files.
                        """
            default = "best"
        }
    }

    multi {
        val centerX: Int by arguments
        val centerY: Int by arguments
        val radius: Int by arguments
        val prefix: String by arguments

        val measureChannel = Channel.Luminance

        val croppedSize = radius * 2 + 1
        val errorMatrix = DoubleMatrix(croppedSize, croppedSize)
        val croppedMatrices = mutableListOf<Matrix>()

        for (fileIndex in inputFiles.indices) {
            println("Loading ${inputFiles[fileIndex]}")
            val inputImage = ImageReader.read(inputFiles[fileIndex])

            val croppedImage = inputImage.cropCenter(radius, centerX, centerY)
            croppedMatrices.add(croppedImage[measureChannel].copy())
        }
        println()

        println("Calculating error between images")
        for (fileIndexY in inputFiles.indices) {
            for (fileIndexX in fileIndexY+1 until inputFiles.size) {
                errorMatrix[fileIndexX, fileIndexY] = croppedMatrices[fileIndexY].averageError(croppedMatrices[fileIndexX])
                errorMatrix[fileIndexY, fileIndexX] = errorMatrix[fileIndexX, fileIndexY]
            }
        }

        println("Finding best image")
        var bestStddev = Double.MAX_VALUE
        var bestFileIndex = 0
        for (fileIndex in inputFiles.indices) {
            val errors = DoubleArray(inputFiles.size * 2)
            for (i in inputFiles.indices) {
                errors[2 * i + 0] = errorMatrix[fileIndex, i]
                errors[2 * i + 1] = -errorMatrix[fileIndex, i]
            }
            val stddev = errors.stddev()
            if (verboseMode) {
                println("Standard Deviation ${inputFiles[fileIndex]} : $stddev")
            }

            if (stddev < bestStddev) {
                bestStddev = stddev
                bestFileIndex = fileIndex
            }
        }
        if (verboseMode) {
            println()
        }

        val bestErrors = mutableMapOf<File, Double>()
        for (fileIndex in inputFiles.indices) {
            bestErrors[inputFiles[fileIndex]] = errorMatrix[bestFileIndex, fileIndex]
        }

        val sortedFiles = inputFiles.sortedBy { f -> bestErrors[f] }

        println("Best ${inputFiles[bestFileIndex]}")
        for (sortedFile in sortedFiles) {
            val error = bestErrors[sortedFile]
            if (verboseMode) {
                println("  $sortedFile : $error")
            }
        }
        if (verboseMode) {
            println()
        }

        for (sortedFileIndex in sortedFiles.indices) {
            val fromFile = sortedFiles[sortedFileIndex]
            val sortPrefix = String.format("${prefix}_%04d_", sortedFileIndex)
            val toFile = fromFile.prefixName(outputDirectory, sortPrefix)

            println("Copying ${fromFile.name} to ${toFile.name}")
            Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        null
    }
}
