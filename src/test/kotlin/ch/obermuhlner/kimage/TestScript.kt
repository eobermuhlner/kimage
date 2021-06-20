package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.nio.file.*
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        //runSingleModeScript("images/align/IMG_7130.TIF")
        runMultiModeScript("images/alignsmall/orion1.png", "images/alignsmall/orion2.png")
    }

    private fun runSingleModeScript(filepath: String) {
        val file = File(filepath)
        val image = ImageReader.read(file)

        singleModeScript(inputFiles = listOf(file), inputFile = file, inputImage = image)
    }

    private fun runMultiModeScript(vararg filepaths: String) {
        val files = filepaths.map { File(it) }

        multiModeScript(inputFiles = files)
    }

    fun singleModeScript(
        kimageVersion: String = "0.1.0",
        verboseMode: Boolean = true,
        inputParameters: Map<String, String> = mapOf(),
        outputDirectory: File = File("."),
        outputPrefix: String = "output",
        multiMode: Boolean = true,
        inputFiles: List<File>,
        inputFile: File,
        inputImage: Image
    ) {
        // BEGIN OF SCRIPT

        inputImage.cropCenter(2491, 3801, 300)

        // END OF SCRIPT
    }

    fun multiModeScript(
        kimageVersion: String = "0.1.0",
        verboseMode: Boolean = true,
        inputParameters: Map<String, String> = mapOf(),
        outputDirectory: File = File("."),
        outputPrefix: String = "output",
        multiMode: Boolean = true,
        inputFiles: List<File>
    ) {
        // BEGIN OF SCRIPT

        require(multiMode)

        println("Align multiple images")

        val files = inputFiles as List<File>
        val parameters = inputParameters as Map<String, String>

        val baseInputFile = files[0]
        println("Loading base image: $baseInputFile")
        val baseImage = ImageReader.readMatrixImage(baseInputFile)
        println("Base image: $baseImage")

        val checkRadius = parameters.getOrDefault("checkRadius", "10").toInt()
        val searchRadius = parameters.getOrDefault("searchRadius", "200").toInt()

        val imageAligner = SimpleImageAligner(checkRadius)
        val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

        val centerX = parameters.getOrDefault("centerX", autoCenterX.toString()).toInt()
        val centerY = parameters.getOrDefault("centerY", autoCenterY.toString()).toInt()

        if (verboseMode) {
            println("Parameters:")
            println("  checkRadius = $checkRadius")
            println("  searchRadius = $searchRadius")
            println("  centerX = $centerX")
            println("  centerY = $centerY")
            println()
        }


        //ImageWriter.write(baseImage.cropCenter(centerX, centerY, checkRadius), File("check_" + baseInputFile.name))

        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")

            val image = ImageReader.readMatrixImage(inputFile)
            println("Aligning image: $image")

            val alignment = imageAligner.align(baseImage, image, centerX = centerX, centerY = centerY, maxOffset = searchRadius)
            println("Alignment: $alignment")

            val alignedImage = image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
            ImageWriter.write(alignedImage, File("aligned_" + inputFile.name))

            //val delta = deltaRGB(baseImage, alignedImage)
            //ImageWriter.write(delta, File("delta_aligned_" + inputFile.name))

            val error = baseImage.averageError(alignedImage)
            println("Standard error to base image: $inputFile $error")
        }

        // END OF SCRIPT
    }
}