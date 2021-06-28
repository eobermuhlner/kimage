import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.huge.HugeFloatArray
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        val orionImages = arrayOf("images/align/orion1.png", "images/align/orion2.png", "images/align/orion3.png", "images/align/orion4.png", "images/align/orion5.png", "images/align/orion6.png", "images/align/orion7.png")
        val alignedOrionImages = arrayOf("images/align/aligned_orion1.png", "images/align/aligned_orion2.png", "images/align/aligned_orion3.png", "images/align/aligned_orion4.png", "images/align/aligned_orion5.png", "images/align/aligned_orion6.png", "images/align/aligned_orion7.png")

        //runScript(scriptAlign(), *orionImages)
        runScript(scriptStackMax(), mapOf(), *orionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *orionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *alignedOrionImages)
        //runScript(scriptRemoveBackgroundMedian(), "images/align/orion1.png")
    }

    private fun scriptRemoveBackgroundMedian(): Script =
        kimage(0.1) {
            name = "remove-background-median"
            description = """
                Removes the background from the input image by subtracting a blurred median of the input.
                
                This script is useful for astrophotography if the image contains mainly stars and not too much nebulas.
                The size of the median filter can be increased to remove stars and nebulas completely.
                
                Use the --debug option to save intermediate images for manual analysis.
                """
            arguments {
                double("removePercent") {
                    description = """
                        The percentage of the calculated background that will be removed.
                        """
                    default = 99.0
                }
                double("medianFilterPercent") {
                    description = """
                        The size of the median filter in percent of the image size.
                        """
                    default = 0.0
                }
                double("blurFilterPercent") {
                    description = """
                        The size of the blur filter in percent of the image size.
                        """
                    default = 0.0
                }
                int("medianFilterSize") {
                    description = """
                        The size of the median filter in pixels.
                        If this value is 0 then the `medianFilterPercent` is used to calculate it.
                        If the `medianFilterPercent` is 0.0 then the median filter size is calculated automatically from the image size.
                        """
                    default = 0
                }
                int("blurFilterSize") {
                    description = """
                        The size of the blur filter in pixels.
                        If this value is 0 then the `blurFilterPercent` is used to calculate it.
                        If the `blurFilterPercent` is 0.0 then the blur filter size is calculated automatically from the image size.
                        """
                    default = 0
                }
            }
            single {
                val removePercent: Double by arguments.double
                val medianFilterPercent: Double by arguments.double
                val blurFilterPercent: Double by arguments.double
                var medianFilterSize: Int by arguments.int
                var blurFilterSize: Int by arguments.int

                val inputImageSize = min(inputImage.width, inputImage.height)
                if (medianFilterSize == 0) {
                    medianFilterSize = if (medianFilterPercent != 0.0) {
                        max(3, (inputImageSize * medianFilterPercent / 100.0).toInt())
                    } else {
                        max(3, inputImageSize.toDouble().pow(0.8).toInt())
                    }
                }
                if (blurFilterSize == 0) {
                    blurFilterSize = if (blurFilterPercent != 0.0) {
                        max(3, (inputImageSize * blurFilterPercent / 100.0).toInt())
                    } else {
                        max(3, medianFilterSize)
                    }
                }

                println("Arguments:")
                println("  removePercent = $removePercent%")
                println("  medianFilterPercent = $medianFilterPercent%")
                println("  blurFilterPercent = $blurFilterPercent%")
                println("  medianFilterSize = $medianFilterSize")
                println("  blurFilterSize = $blurFilterSize")

                if (verboseMode) println("Running median filter ...")
                val medianImage = inputImage.medianFilter(medianFilterSize)
                if (debugMode) {
                    val medianFile = File("median_" + inputFile.name)
                    println("Writing $medianFile")
                    ImageWriter.write(medianImage, medianFile)
                }

                if (verboseMode) println("Running gaussian blur filter ...")
                val backgroundImage = medianImage.gaussianBlurFilter(blurFilterSize)
                if (debugMode) {
                    val backgroundFile = File("background_" + inputFile.name)
                    println("Writing $backgroundFile")
                    ImageWriter.write(backgroundImage, backgroundFile)
                }

                if (verboseMode) println("Subtracting $removePercent% background glow from original image ...")
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

                Use the --debug option to save intermediate images for manual analysis.
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

                if (debugMode) {
                    val checkImage = baseImage.cropCenter(checkRadius, centerX, centerY)
                    val checkFile = File("check_" + baseInputFile.name)
                    println("Saving $checkFile for manual analysis")
                    ImageWriter.write(checkImage, checkFile)
                    println()
                }

                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")

                    val image = ImageReader.readMatrixImage(inputFile)
                    if (verboseMode) println("Aligning image: $image")

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

                    if (debugMode) {
                        val deltaFile = File("delta_${prefix}_" + inputFile.name)
                        println("Saving $deltaFile for manual analysis")
                        val deltaImage = deltaRGB(baseImage, alignedImage)
                        ImageWriter.write(deltaImage, deltaFile)
                    }

                    println()
                }
            }
        }

    fun scriptStackMax(): Script =
        kimage(0.1) {
            name = "stack-max"
            description = """
                Stacks multiple images by calculating a pixel-wise maximum.
                
                This stacking script is useful to find outliers and badly aligned images.
                """
            arguments {
            }

            multi {
                println("Stack multiple images using max")

                inputFiles.map {
                    println("Loading image: $it")
                    ImageReader.readMatrixImage(it) as Image
                } .reduce { stacked, img ->
                    max(stacked, img.crop(0, 0, stacked.width, stacked.height))
                }
            }
        }


    fun scriptStack() =
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

                val sigmaClipHistogram = Histogram(inputFiles.size)

                val stackingMethod: (FloatArray) -> Float = when(method) {
                    "median" -> { array -> array.median() }
                    "average" -> { array -> array.average() }
                    "max" -> { array -> array.maxOrNull()!! }
                    "min" -> { array -> array.minOrNull()!! }
                    "sigmaclip-median" -> { array -> array.sigmaClip(kappa = kappa.toFloat(), iterations = iterations, histogram = sigmaClipHistogram).median() }
                    "sigmaclip-average" -> { array -> array.sigmaClip(kappa = kappa.toFloat(), iterations = iterations, histogram = sigmaClipHistogram).average() }
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
                println()

                if (sigmaClipHistogram.n > 0) {
                    println("Sigma-Clip Histogram")
                    for (i in sigmaClipHistogram.indices) {
                        val length = 40 * sigmaClipHistogram[i] / sigmaClipHistogram.n
                        val line = String.format("%3d : %10d %s", i, sigmaClipHistogram[i], "#".repeat(length))
                        println("  $line")
                    }
                    println()
                }

                resultImage
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