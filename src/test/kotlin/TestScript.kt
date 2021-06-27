import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*

import java.io.*
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        //runScript(scriptAlign(), "images/align/orion1.png", "images/align/orion2.png", "images/animal.png")
        runScript(scriptRemoveBackgroundMedian(), "images/align/orion1.png", "images/align/orion2.png", "images/animal.png")
    }

    private fun scriptRemoveBackgroundMedian(): Script =
        kimage(0.1) {
            arguments {
                double("removePercent") {
                    default = 99.0
                }
                double("medianKernelPercent") {
                    default = 1.0
                }
                double("blurKernelPercent") {
                    default = 2.0
                }
            }
            single {
                val removePercent by arguments.double
                val medianKernelPercent by arguments.double
                val blurKernelPercent by arguments.double

                println("Arguments:")
                println("  removePercent = $removePercent%")
                println("  medianKernelPercent = $medianKernelPercent%")
                println("  blurKernelPercent = $blurKernelPercent%")

                val medianKernelSize = max(1, (min(inputImage.width, inputImage.height) * medianKernelPercent / 100.0).toInt())
                val blurKernelSize = max(1, (min(inputImage.width, inputImage.height) * blurKernelPercent / 100.0).toInt())

                if (arguments.verboseMode) {
                    println("  -> calculated medianKernelSize = $medianKernelSize pixels")
                    println("  -> calculated blurKernelSize = $blurKernelSize pixels")
                }

                if (arguments.verboseMode) { println("Running median filter ...")
                }
                val medianImage = inputImage.medianFilter(medianKernelSize)
                if (arguments.debugMode) {
                    val medianFile = File("median_" + inputFile.name)
                    println("Writing $medianFile")
                    ImageWriter.write(medianImage, medianFile)
                }

                if (arguments.verboseMode) println("Running gaussian blur filter ...")
                val backgroundImage = medianImage.gaussianBlurFilter(blurKernelSize)
                if (arguments.debugMode) {
                    val backgroundFile = File("background_" + inputFile.name)
                    println("Writing $backgroundFile")
                    ImageWriter.write(backgroundImage, backgroundFile)
                }

                if (arguments.verboseMode) println("Subtracting $removePercent% background glow from original image ...")
                inputImage - backgroundImage * (removePercent/100.0)
            }
        }

    private fun scriptAlign(): Script =
        kimage(0.1) {
            name = "align"
            description = """
                Align multiple images.
                The base image is the first image argument.
                The remaining image arguments are aligned to the base image by searching for a matching feature.
                
                The feature to match is defined by the centerX/centerY coordinates in the base image and the check radius.
                The searchRadius defines how far the matching feature is searched.
                """
            arguments {
                int("checkRadius") {
                    description = """
                        The radius to check for similarity.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                int("searchRadius") {
                    description = """
                        The search radius defining the maximum offset to align.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                int("centerX") {
                    description = """
                        The X coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                int("centerY") {
                    description = """
                        The Y coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                double("errorThreshold") {
                    description = """
                        The maximum error threshold for storing an aligned image.
                        Images with an error above the error threshold will be either ignored
                        or saved with a different prefix.
                        See `saveBad`, `prefixBad`.
                        """
                    min = 0.0
                    default = 1E-3
                }
                string("prefix") {
                    description = "The prefix of the aligned output files."
                    default = "aligned"
                }
                boolean("saveBad") {
                    description = "Controls whether badly aligned images are saved."
                    default = true
                }
                string("prefixBad") {
                    description = "The prefix of the badly aligned output files."
                    default = "badaligned"
                }
            }

            multi {
                println("inputFiles : $inputFiles")

                val baseInputFile = inputFiles[0]
                println("Loading base image: $baseInputFile")
                val baseImage = ImageReader.readMatrixImage(baseInputFile)
                println("Base image: $baseImage")
                println()

                val baseImageMinSize = min(baseImage.width, baseImage.height)
                val defaultCheckRadius = sqrt(baseImageMinSize.toDouble()).toInt()
                val defaultSearchRadius = min(baseImageMinSize, defaultCheckRadius * 4)

                val checkRadius: Int by arguments.int.withDefault { defaultCheckRadius }
                val searchRadius: Int by arguments.int.withDefault { defaultSearchRadius }

                val imageAligner = ImageAligner(checkRadius)
                val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

                val centerX: Int by arguments.int.withDefault { autoCenterX }
                val centerY: Int by arguments.int.withDefault { autoCenterY }
                val errorThreshold: Double by arguments.double
                val prefix: String by arguments.string
                val saveBad: Boolean by arguments.boolean
                val prefixBad: String by arguments.string

                println("Arguments:")
                println("  checkRadius = $checkRadius")
                println("  searchRadius = $searchRadius")
                println("  centerX = $centerX")
                println("  centerY = $centerY")
                println("  prefix = $prefix")
                println("  saveBad = $saveBad")
                println("  prefixBad = $prefixBad")
                println()

                if (arguments.debugMode) {
                    val checkImage = baseImage.cropCenter(checkRadius, centerX, centerY)
                    val checkFile = File("check_" + baseInputFile.name)
                    println("Saving $checkFile for manual analysis")
                    ImageWriter.write(checkImage, checkFile)
                    println()
                }

                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")

                    val image = ImageReader.readMatrixImage(inputFile)
                    if (arguments.verboseMode) println("Aligning image: $image")

                    val alignment = imageAligner.align(baseImage, image, centerX = centerX, centerY = centerY, maxOffset = searchRadius)
                    println("Alignment: $alignment")

                    val alignedImage = image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)

                    val error = baseImage.averageError(alignedImage)
                    if (error <= errorThreshold) {
                        val alignedFile = File("${prefix}_" + inputFile.name)
                        println("Error $error <= $errorThreshold : saving $alignedFile")
                        ImageWriter.write(alignedImage, alignedFile)
                    } else {
                        if (saveBad) {
                            val badalignedFile = File("${prefixBad}_" + inputFile.name)
                            println("Error $error > $errorThreshold : saving $badalignedFile")
                            ImageWriter.write(alignedImage, badalignedFile)
                        } else {
                            println("Error $error > $errorThreshold : ignoring badly aligned image")
                        }
                    }

                    if (arguments.debugMode) {
                        val deltaFile = File("delta_$prefix" + inputFile.name)
                        println("Saving $deltaFile for manual analysis")
                        val deltaImage = deltaRGB(baseImage, alignedImage)
                        ImageWriter.write(deltaImage, deltaFile)
                    }

                    println()
                }
            }
        }

    fun runScript(script: Script, vararg filepaths: String) {
        runScript(script, mapOf(), *filepaths)
    }

    fun runScript(script: Script, arguments: Map<String, String>, vararg filepaths: String) {
        runScript(script, arguments, filepaths.map { File(it) })
    }

    fun runScript(script: Script, arguments: Map<String, String>, files: List<File>) {
        ScriptExecutor.executeScript(script, arguments, files, true, true, true , "output", "");
        ScriptExecutor.executeScript(script, arguments, files, false, true, true, "output", "");
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
}