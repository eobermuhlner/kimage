import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.huge.HugeFloatArray
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.io.ImageReader.read
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        val orionImages = arrayOf("images/align/orion1.png", "images/align/orion2.png", "images/align/orion3.png", "images/align/orion4.png", "images/align/orion5.png", "images/align/orion6.png", "images/align/orion7.png")
        val alignedOrionImages = arrayOf("images/align/aligned_orion1.png", "images/align/aligned_orion2.png", "images/align/aligned_orion3.png", "images/align/aligned_orion4.png", "images/align/aligned_orion5.png", "images/align/aligned_orion6.png", "images/align/aligned_orion7.png")

        //runScript(scriptAlign(), *orionImages)
        //runScript(scriptStackMax(), mapOf(), *orionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *orionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *alignedOrionImages)
        //runScript(scriptRemoveBackgroundMedian(), "images/align/orion1.png")
        //runScript(scriptHistogram(), "images/align/output_orion1.png")
        //runScript(scriptColorStretch(), "images/align/output_orion1.png")
        runScript(scriptCalibrate())

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
                val removePercent: Double by arguments
                val medianFilterPercent: Double by arguments
                val blurFilterPercent: Double by arguments
                var medianFilterSize: Int by arguments
                var blurFilterSize: Int by arguments

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
                    val medianFile = inputFile.prefixName("median_")
                    println("Writing $medianFile")
                    ImageWriter.write(medianImage, medianFile)
                }

                if (verboseMode) println("Running gaussian blur filter ...")
                val backgroundImage = medianImage.gaussianBlurFilter(blurFilterSize)
                if (debugMode) {
                    val backgroundFile = inputFile.prefixName("background_")
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
                val baseImage = read(baseInputFile)
                println("Base image: $baseImage")
                println()

                val baseImageMinSize = min(baseImage.width, baseImage.height)
                val defaultCheckRadius = sqrt(baseImageMinSize.toDouble()).toInt()
                val defaultSearchRadius = min(baseImageMinSize, defaultCheckRadius * 4)

                val checkRadius: Int by arguments.withDefault { defaultCheckRadius }
                val searchRadius: Int by arguments.withDefault { defaultSearchRadius }

                val imageAligner = ImageAligner(checkRadius)
                val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

                val centerX: Int by arguments.withDefault { autoCenterX }
                val centerY: Int by arguments.withDefault { autoCenterY }
                val errorThreshold: Double by arguments
                val prefix: String by arguments
                val saveBad: Boolean by arguments
                val prefixBad: String by arguments

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
                    val checkFile = baseInputFile.prefixName("check_")
                    println("Saving $checkFile for manual analysis")
                    ImageWriter.write(checkImage, checkFile)
                    println()
                }

                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")

                    val image = read(inputFile)
                    if (verboseMode) println("Aligning image: $image")

                    val alignment = imageAligner.align(
                        baseImage,
                        image,
                        centerX = centerX,
                        centerY = centerY,
                        maxOffset = searchRadius
                    )
                    println("Alignment: $alignment")

                    val alignedImage = image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)

                    val error = baseImage.averageError(alignedImage)
                    if (error <= errorThreshold) {
                        val alignedFile = inputFile.prefixName("${prefix}_")
                        println("Error $error <= $errorThreshold : saving $alignedFile")
                        ImageWriter.write(alignedImage, alignedFile)
                    } else {
                        if (saveBad) {
                            val badalignedFile = inputFile.prefixName("${prefixBad}_")
                            println("Error $error > $errorThreshold : saving $badalignedFile")
                            ImageWriter.write(alignedImage, badalignedFile)
                        } else {
                            println("Error $error > $errorThreshold : ignoring badly aligned image")
                        }
                    }

                    if (debugMode) {
                        val deltaFile = inputFile.prefixName("delta_${prefix}_")
                        println("Saving $deltaFile for manual analysis")
                        val deltaImage = deltaChannel(baseImage, alignedImage)
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

                var stacked: Image? = null
                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")
                    val image = read(inputFile)
                    stacked = if (stacked == null) {
                        image
                    } else {
                        max(stacked, image)
                    }
                }

                stacked
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
                        
                        The method `sigma-clip-median` removes outliers before using `median` on the remaining values.
                        The method `sigma-clip-average` removes outliers before using `average` on the remaining values.
                        The method `sigma-winsorize-median` replaces outliers with the nearest value in sigma range before using `median`.
                        The method `sigma-winsorize-average` replaces outliers with the nearest value in sigma range before using `average`.
                        The method `winsorized-sigma-clip-median` replaces outliers with the nearest value in sigma range before sigma-clipping and then using `median`.
                        The method `winsorized-sigma-clip-average` replaces outliers with the nearest value in sigma range before sigma-clipping and then using `average`.
                        
                        All methods that use sigma-clipping print a histogram with the information how many input values where actually used to stack each output value. 
                        """
                    allowed = listOf("median", "average", "max", "min", "sigma-clip-median", "sigma-clip-average", "sigma-winsorize-median", "sigma-winsorize-average", "winsorized-sigma-clip-median", "winsorized-sigma-clip-average", "all")
                    default = "sigma-clip-median"
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

                val method: String by arguments
                val kappa: Double by arguments
                val iterations: Int by arguments

                println("Arguments:")
                println("  method = $method")
                println("  kappa = $kappa")
                println("  iterations = $iterations")
                println()

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

                    val outputFile = inputFiles[0].prefixName("stack(${method})_")
                    println("Saving $outputFile")
                    ImageWriter.write(resultImage, outputFile)

                    println()
                }

                null
            }
        }

    fun scriptHistogram(): Script =
        kimage(0.1) {
            name = "histogram"
            description = """
                Creates a histogram image.
                """
            arguments {
                int("width") {
                    description = """
                        The width of the histogram.                        
                    """
                    default = 512
                }
                int("height") {
                    description = """
                        The height of the histogram.                        
                    """
                    default = 300
                }
            }

            single {
                println("Create a histogram of an image.")
                println()

                val width: Int by arguments
                val height: Int by arguments

                println("Arguments:")
                println("  width = $width")
                println("  height = $height")
                println()

                inputImage.histogramImage(width, height)
            }
        }

    // https://clarkvision.com/articles/astrophotography-rnc-color-stretch/
    fun scriptColorStretch(): Script =
        kimage(0.1) {
            name = "color-stretch"
            description = """
                Stretches the colors of an image to fill the entire value range.
                """
            arguments {
                double("brightness") {
                    description = """
                        The power value of the brightness increase.
                        
                        A power value > 1 increases the brightness.
                        A power value = 0 does not change the brightness.
                        A power value < 1 increases the brightness.
                        """
                    min = 0.0
                    default = 2.0
                }
                string("curve") {
                    description = """
                        The curve shape used to modify the contrast.
                        """
                    allowed = listOf("linear", "s-curve", "s-curve-bright", "s-curve-dark", "bright+", "dark+", "bright-", "dark-", "custom1", "custom2")
                    default = "s-curve"
                }
                double("custom1X") {
                    default = 0.2
                }
                double("custom1Y") {
                    default = 0.1
                }
                double("custom2X") {
                    default = 0.8
                }
                double("custom2Y") {
                    default = 0.9
                }
            }

            single {
                println("Color stretching")
                println()

                val brightness: Double by arguments
                val curve: String by arguments
                val custom1X: Double by arguments
                val custom1Y: Double by arguments
                val custom2X: Double by arguments
                val custom2Y: Double by arguments

                val histogramWidth = 256
                val histogramHeight = 150

                println("Arguments:")
                println("  brightness = $brightness")
                println("  curve = $curve")
                when (curve) {
                    "custom1" -> {
                        println("  custom1X = $custom1X")
                        println("  custom1Y = $custom1Y")
                    }
                    "custom2" -> {
                        println("  custom1X = $custom1X")
                        println("  custom1Y = $custom1Y")
                        println("  custom2X = $custom2X")
                        println("  custom2Y = $custom2Y")
                    }
                }
                println()

                val (power1, power2) = if (brightness < 1000.0) {
                    Pair(brightness, 1.0)
                } else {
                    Pair(brightness.pow(1.0 / 5.0), 5.0)
                }

                var image = inputImage

                if (debugMode) {
                    println("Image average: ${image.values().average()}")
                    println("Image median: ${image.values().fastMedian()}")
                    println("Image stddev: ${image.values().stddev()}")

                    val histogramInputFile = inputFile.prefixName("hist_input_")
                    println("Saving $histogramInputFile for manual analysis")
                    ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramInputFile)
                    println()
                }

                if (power1 != 1.0) {
                    image = image.onEach { v -> v.pow(1.0 / power1) }
                }
                if (power2 != 1.0) {
                    image = image.onEach { v -> v.pow(1.0 / power2) }
                }

                if (debugMode) {
                    println("Image average: ${image.values().average()}")
                    println("Image median: ${image.values().fastMedian()}")
                    println("Image stddev: ${image.values().stddev()}")

                    val histogramBrightnessFile = inputFile.prefixName("hist_brightness_")
                    println("Saving $histogramBrightnessFile (after brightness correction) for manual analysis")
                    ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramBrightnessFile)
                    println()
                }

                val (curvePointsX, curvePointsY) = when(curve) {
                    "linear" -> {
                        Pair(
                            listOf(0.0, 1.0),
                            listOf(0.0, 1.0)
                        )
                    }
                    "s-curve" -> {
                        Pair(
                            listOf(0.0, 0.3, 0.7, 1.0),
                            listOf(0.0, 0.2, 0.8, 1.0)
                        )
                    }
                    "s-curve-bright" -> {
                        Pair(
                            listOf(0.0, 0.2,  0.7, 1.0),
                            listOf(0.0, 0.18, 0.8, 1.0)
                        )
                    }
                    "s-curve-dark" -> {
                        Pair(
                            listOf(0.0, 0.3, 0.7, 1.0),
                            listOf(0.0, 0.2, 0.72, 1.0)
                        )
                    }
                    "bright+" -> {
                        Pair(
                            listOf(0.0, 0.6, 1.0),
                            listOf(0.0, 0.7, 1.0)
                        )
                    }
                    "dark+" -> {
                        Pair(
                            listOf(0.0, 0.4, 1.0),
                            listOf(0.0, 0.5, 1.0)
                        )
                    }
                    "bright-" -> {
                        Pair(
                            listOf(0.0, 0.6, 1.0),
                            listOf(0.0, 0.5, 1.0)
                        )
                    }
                    "dark-" -> {
                        Pair(
                            listOf(0.0, 0.4, 1.0),
                            listOf(0.0, 0.3, 1.0)
                        )
                    }
                    "custom1" -> {
                        Pair(
                            listOf(0.0, custom1X, 1.0),
                            listOf(0.0, custom1Y, 1.0)
                        )
                    }
                    "custom2" -> {
                        Pair(
                            listOf(0.0, custom1X, custom2X, 1.0),
                            listOf(0.0, custom1X, custom2Y, 1.0)
                        )
                    }
                    else -> throw IllegalArgumentException("Unknown curve: $curve")
                }

                println("Curve Points:")
                println("  X: $curvePointsX")
                println("  Y: $curvePointsY")
                println()

                val spline: SplineInterpolator = SplineInterpolator.createMonotoneCubicSpline(curvePointsX, curvePointsY)

                image = image.onEach { v -> spline.interpolate(v) }

                if (debugMode) {
                    println("Image average: ${image.values().average()}")
                    println("Image median: ${image.values().fastMedian()}")
                    println("Image stddev: ${image.values().stddev()}")

                    val histogramOutputFile = inputFile.prefixName("hist_output_")
                    println("Saving $histogramOutputFile (after brightness correction) for manual analysis")
                    ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramOutputFile)
                    println()
                }

                image
            }
        }

    fun scriptCalibrate(): Script =
        kimage(0.1) {
            name = "calibrate"
            description = """
                Calibrates bias/dark/flat/darkflat/light images.
                """
            arguments {
            }

            multi {
                println("Calibrate")
                println()

                println("Loading bias")
                val bias = ImageReader.read(File("images/calibrate/bias.TIF"))
                println("Loading dark")
                var dark = ImageReader.read(File("images/calibrate/dark.TIF"))
                println("Loading darkflat")
                var darkflat = ImageReader.read(File("images/calibrate/darkflat.TIF"))
                println("Loading flat")
                var flat = ImageReader.read(File("images/calibrate/flat.TIF"))

                dark = dark - bias
                darkflat = darkflat - bias
                flat = flat - bias - darkflat

                for (inputFile in inputFiles) {
                    println("Loading $inputFile")
                    var light = ImageReader.read(inputFile)

                    val light2 = light - bias - dark
                    ImageWriter.write(deltaChannel(light2, light, factor = 20.0), inputFile.prefixName("delta_light2_"))

                    val light3 = light2.pixelWiseDiv(flat)
                    ImageWriter.write(light3, inputFile.prefixName("calibrated_"))
                    ImageWriter.write(deltaChannel(light3, light2), inputFile.prefixName("delta_light3_"))
                }

                null
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