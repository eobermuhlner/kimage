package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        //runSingleModeScript("images/align/orion1.png")
        //runMultiModeScript("images/align/aligned_orion1.png", "images/align/aligned_orion2.png")

        runMultiModeScriptDSL("images/align/aligned_orion1.png", "images/align/aligned_orion2.png")

    }

    private fun runSingleModeScript(filepath: String) {
        val file = File(filepath)
        val image = ImageReader.read(file)

        singleModeScript(inputFiles = listOf(file), inputFile = file, inputImage = image)
    }

    private fun runMultiModeScript(vararg filepaths: String) {
        val files: List<Any> = filepaths.map { File(it) }

        multiModeScript(inputFiles = files)
    }

    fun singleModeScript(
        kimageVersion: String = "0.1.0",
        verboseMode: Boolean = true,
        inputParameters: Map<String, String> = mapOf(),
        outputDirectory: File = File("."),
        outputPrefix: String = "output",
        singleMode: Boolean = true,
        multiMode: Boolean = false,
        inputFiles: List<File>,
        inputFile: File,
        inputImage: Image
    ) {
        // BEGIN OF SCRIPT

        require(singleMode)


        // END OF SCRIPT
    }

    fun multiModeScript(
        kimageVersion: String = "0.1.0",
        verboseMode: Boolean = true,
        inputParameters: Map<String, String> = mapOf(),
        outputDirectory: File = File("."),
        outputPrefix: String = "output",
        singleMode: Boolean = false,
        multiMode: Boolean = true,
        inputFiles: List<Any>
    ) {
        // BEGIN OF SCRIPT


        // END OF SCRIPT

        //ImageWriter.write(resultImage, File("testscript.png"))
    }

    fun runMultiModeScriptDSL(vararg filepaths: String) {

        val script = kimage {
            name = "align"
            description = "Align images to a base image."

            arg {
                name = "checkRadius"
                description = "The radius to check for similarity."
            }
            arg {
                name = "searchRadius"
                description = "The search radius defining the maximum offset to align."
            }
            arg {
                name = "centerX"
                description = "The X coordinate of the center to check for alignment."
            }
            arg {
                name = "centerY"
                description = "The Y coordinate of the center to check for alignment."
            }

            multi {
                println("inputFiles : $inputFiles")

                val baseInputFile = inputFiles[0]
                println("Loading base image: $baseInputFile")
                val baseImage = ImageReader.readMatrixImage(baseInputFile)
                println("Base image: $baseImage")
                println()

                val defaultCheckRadius = min(baseImage.width, baseImage.height) / 10
                val defaultSearchRadius = defaultCheckRadius * 5

                val checkRadius = arguments.getOrDefault("checkRadius", defaultCheckRadius.toString()).toInt()
                val searchRadius = arguments.getOrDefault("searchRadius", defaultSearchRadius.toString()).toInt()

                val imageAligner = ImageAligner(checkRadius)
                val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

                val centerX = arguments.getOrDefault("centerX", autoCenterX.toString()).toInt()
                val centerY = arguments.getOrDefault("centerY", autoCenterY.toString()).toInt()

                println("Arguments:")
                println("  checkRadius = $checkRadius")
                println("  searchRadius = $searchRadius")
                println("  centerX = $centerX")
                println("  centerY = $centerY")
                println()

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
                    println()
                }
            }
        }

        script.help()
        script.runMulti(filepaths.map { File(it) }, mapOf())
    }
}