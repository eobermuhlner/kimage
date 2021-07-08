import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        val orionImages = arrayOf("images/align/orion1.png", "images/align/orion2.png", "images/align/orion3.png", "images/align/orion4.png", "images/align/orion5.png", "images/align/orion6.png")
        val alignedOrionImages = arrayOf("images/align/aligned_orion1.png", "images/align/aligned_orion2.png", "images/align/aligned_orion3.png", "images/align/aligned_orion4.png", "images/align/aligned_orion5.png", "images/align/aligned_orion6.png")
        //val hdrImages = arrayOf("images/hdr/hdr1.jpg", "images/hdr/hdr2.jpg", "images/hdr/hdr3.jpg", "images/hdr/hdr4.jpg")
        val hdrImages = arrayOf("images/hdr/HDRI_Sample_Scene_Window_-_01.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_02.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_03.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_04.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_05.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_06.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_07.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_08.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_09.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_10.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_11.jpg", "images/hdr/HDRI_Sample_Scene_Window_-_12.jpg")

        //runScript(scriptAlign(), *orionImages)
        //runScript(scriptStackMax(), mapOf(), *orionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *alignedOrionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *orionImages)
        //runScript(scriptRemoveBackgroundMedian(),"images/align/orion1.png")
        //runScript(scriptHistogram(), "images/align/output_orion1.png")
        //runScript(scriptColorStretch(), "images/align/output_orion1.png")
        //runScript(scriptCalibrate())
        //runScript(scriptHDR(), mapOf(), *alignedOrionImages)
        //runScript(scriptHDR(), mapOf(), *hdrImages)
        //runScript(scriptCalibrate(), mapOf("biasDir" to "images/align"))
        //runScript(scriptCalibrate(), mapOf("biasDir" to "images/align"))

        //runScript(scriptRemoveVignette(), mapOf(), "images/flat_large.tif")
        runScript(scriptRemoveVignette(), mapOf(), "images/calibrate/light/IMG_6800.TIF")
        //runScript(scriptRemoveVignette(), mapOf(), "images/align/orion1.png")

        //runScript(scriptTestMulti(), mapOf())
    }

    private fun scriptRemoveBackgroundMedian(): Script =
        kimage(0.1) {
            name = "remove-background-median"
            title = "Remove the background by subtracting a blurred median-filtered version of the input"
            description = """
                This script is useful for astrophotography if the image contains mainly stars and not too much nebulas.
                The size of the median filter can be increased to remove stars and nebulas completely.
                
                Use the --debug option to save intermediate images for manual analysis.
                """
            arguments {
                double("removePercent") {
                    description = """
                        The percentage of the calculated background that will be removed.
                        """
                    default = 100.0
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
            title = "Align multiple images"
            description = """
                The base image is the first image argument.
                The remaining image arguments are aligned to the base image by searching for a matching feature.
                
                The feature to match is defined by the `centerX`/`centerY` coordinates in the base image and the `checkRadius`.
                The `searchRadius` defines how far the matching feature is searched.

                Use the --debug option to save intermediate images for manual analysis.
                """
            arguments {
                optionalInt("checkRadius") {
                    description = """
                        The radius to check for similarity.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                optionalInt("searchRadius") {
                    description = """
                        The search radius defining the maximum offset to align.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                optionalInt("centerX") {
                    description = """
                        The X coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
                    min = 0
                }
                optionalInt("centerY") {
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
                    default = 0.001
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
                val baseInputFile = inputFiles[0]
                println("Loading base image: $baseInputFile")
                val baseImage = ImageReader.read(baseInputFile)
                println("Base image: $baseImage")
                println()

                val baseImageMinSize = min(baseImage.width, baseImage.height)
                val defaultCheckRadius = sqrt(baseImageMinSize.toDouble()).toInt()
                val defaultSearchRadius = min(baseImageMinSize, defaultCheckRadius * 4)

                var checkRadius: Optional<Int> by arguments
                var searchRadius: Optional<Int> by arguments
                if (!checkRadius.isPresent) {
                    checkRadius = Optional.of(defaultCheckRadius)
                }
                if (!searchRadius.isPresent) {
                    searchRadius = Optional.of(defaultSearchRadius)
                }

                val imageAligner = ImageAligner(checkRadius.get())
                val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

                var centerX: Optional<Int> by arguments
                var centerY: Optional<Int> by arguments
                if (!centerX.isPresent) {
                    centerX = Optional.of(autoCenterX)
                }
                if (!centerY.isPresent) {
                    centerY = Optional.of(autoCenterY)
                }

                val errorThreshold: Double by arguments
                val prefix: String by arguments
                val saveBad: Boolean by arguments
                val prefixBad: String by arguments

                println("Arguments (calculated from input):")
                println("  checkRadius = $checkRadius")
                println("  searchRadius = $searchRadius")
                println("  centerX = $centerX")
                println("  centerY = $centerY")
                println()

                if (debugMode) {
                    val checkImage = baseImage.cropCenter(checkRadius.get(), centerX.get(), centerY.get())
                    val checkFile = baseInputFile.prefixName("check_")
                    println("Saving $checkFile for manual analysis")
                    ImageWriter.write(checkImage, checkFile)
                    println()
                }

                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")

                    val image = ImageReader.read(inputFile)
                    if (verboseMode) println("Aligning image: $image")

                    val alignment = imageAligner.align(
                        baseImage,
                        image,
                        centerX = centerX.get(),
                        centerY = centerY.get(),
                        maxOffset = searchRadius.get()
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
            title = "Stack multiple images by calculating a pixel-wise maximum"
            description = """
                This stacking script is useful to find outliers and badly aligned images.
                This implementation is faster and uses less memory than using the generic script `stack --arg method=max`.
                """
            arguments {
            }

            multi {
                println("Stack multiple images using max")

                var stacked: Image? = null
                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")
                    val image = ImageReader.read(inputFile)
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
                    allowed = listOf("median", "average", "max", "min", "sigma-clip-median", "sigma-clip-average", "sigma-winsorize-median", "sigma-winsorize-average", "winsorized-sigma-clip-median", "winsorized-sigma-clip-average", "weighted-average", "all")
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
                        "weighted-average" -> { array ->
                            array.weightedAverage({ i, value ->
                                val wellExposed = exp(-(value - 0.5f).pow(2)/0.08f)
                                wellExposed.pow(1)
                            })
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

    fun scriptHDR() =
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

    fun scriptHistogram(): Script =
        kimage(0.1) {
            name = "histogram"
            title = "Create a histogram image"
            description = """
                The values of every channel (RGB) are counted and a histogram is created for each channel.
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
                val width: Int by arguments
                val height: Int by arguments

                inputImage.histogramImage(width, height)
            }
        }

    // https://clarkvision.com/articles/astrophotography-rnc-color-stretch/
    fun scriptColorStretch(): Script =
        kimage(0.1) {
            name = "color-stretch"
            title = "Stretch the colors of an image to fill the entire value range"
            description = """
                The colors are first brightened and then a curve is applied.
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
                val brightness: Double by arguments
                val curve: String by arguments
                val custom1X: Double by arguments
                val custom1Y: Double by arguments
                val custom2X: Double by arguments
                val custom2Y: Double by arguments

                val histogramWidth = 256
                val histogramHeight = 150

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

                    val outputFile = inputFile.prefixName("color-stretch(${brightness},${curve})_")
                    println("Saving $outputFile")
                    ImageWriter.write(image, outputFile)
                }

                null
            }
        }

    fun scriptCalibrate(): Script =
        kimage(0.1) {
            name = "calibrate"
            description = """
                Calibrates images using bias/dark/flat/darkflat images.
                
                The different calibration files are optional, specify only the calibration image you have.
                
                ### Creating Calibration Images
                
                Create about 20-50 images of each calibration image type.
                
                - `bias` images 
                  - camera with lens cap on
                  - same ISO as for real pictures
                  - fastest exposure time
                - `flat` images
                  - camera against homogeneous light source (e.g. t-shirt over lens against sky)
                  - same objective + focus as for real pictures
                  - same aperture as for real pictures
                  - set exposure time so that histogram shows most pixels at ~50%
                - `darkflat` images
                  - camera with lens cap on
                  - same objective + focus as for real pictures
                  - same aperture as for real pictures
                  - same ISO as for `flat` images
                  - same exposure time as for `flat` images
                - `dark` images
                  - camera with lens cap on
                  - same objective + focus as for real pictures
                  - same aperture as for real pictures
                  - same ISO as for real pictures
                  - same exposure time as for real pictures
                  - same temperature as for real pictures
                  - (usually take the dark pictures immediately after taking the real pictures)
                
                Stack the `bias` images with:
                
                    kimage stack --arg method=median bias*.TIF
                The output will be your master `bias` image - rename it accordingly.
                
                Calibrate all other images with the `bias` images and stack them.
                
                For example the `flat` images:
                
                    kimage calibrate --arg bias=master_bias.TIF flat*.TIF
                    kimage stack --arg method=median calibrate_flat*.TIF
                
                Do this for the `flat`, `darkflat` and `dark` images.
                The outputs will be your master `flat`, `darkflat` and `dark` images - rename them accordingly.
                
                Calibrate the real images:
                
                    kimage calibrate --arg bias=master_bias.TIF --arg flat=master_flat.TIF --arg darkflat=master_darkflat.TIF --arg dark=master_dark.TIF light*.TIF
                    
                See: http://deepskystacker.free.fr/english/theory.htm
                """
            arguments {
                optionalFile("biasDir") {
                    description = "Directory containing bias images"
                    isDirectory = true
                }
                string("biasFilePattern") {
                    default = "*.{tif,tiff,png,jpg,jpeg}"
                }
                optionalImage("bias") {
                    description = """
                The `bias` master calibration image.
                
                This argument is optional.
                If no `bias` image is specified it will not be used in the calibration process.
                """
                }
                optionalImage("dark") {
                    description = """
                The `dark` master calibration image.
                
                This argument is optional.
                If no `dark` image is specified it will not be used in the calibration process.
                """
                }
                optionalImage("flat") {
                    description = """
                The `flat` master calibration image.
                
                This argument is optional.
                If no `flat` image is specified it will not be used in the calibration process.
                """
                }
                optionalImage("darkflat") {
                    description = """
                The `darkflat` master calibration image.
                
                This argument is optional.
                If no `flat` image is specified it will not be used in the calibration process.
                """
                }
            }

            multi {
                val biasDir: Optional<File> by arguments
                val biasFilePattern: String by arguments

                var bias: Optional<Image> by arguments
                var dark: Optional<Image> by arguments
                var flat: Optional<Image> by arguments
                var darkflat: Optional<Image> by arguments
                val applyBiasOnCalibration = false

                if (biasDir.isPresent) {
                    val biasFilePatternMatcher = FileSystems.getDefault().getPathMatcher("glob:$biasFilePattern")

                    val biasFiles = biasDir.get().listFiles { f ->
                        biasFilePatternMatcher.matches(Path.of(f.name))
                    }

                    for (biasFile in biasFiles) {
                        val biasImage = ImageReader.read(biasFile)
                    }
                }

                if (applyBiasOnCalibration && bias.isPresent) {
                    if (dark.isPresent) {
                        dark = Optional.of(dark.get() - bias.get())
                    }
                    if (darkflat.isPresent) {
                        darkflat = Optional.of(darkflat.get() - bias.get())
                    }
                    if (flat.isPresent) {
                        flat = Optional.of(flat.get() - bias.get())
                    }
                }

                if (darkflat.isPresent) {
                    flat = Optional.of(flat.get() - darkflat.get())
                }

                for (inputFile in inputFiles) {
                    println("Loading $inputFile")
                    var light = ImageReader.read(inputFile)

                    if (bias.isPresent) {
                        light = light - bias.get()
                    }
                    if (dark.isPresent) {
                        light = light - dark.get()
                    }
                    if (flat.isPresent) {
                        light = light / flat.get()
                    }

                    ImageWriter.write(light, inputFile.prefixName("calibrated_"))
                }

                null
            }
        }

    fun scriptRemoveBackgroundGradient(): Script =
        kimage(0.1) {
            name = "remove-background-gradient"
            title = "Remove the background by subtracting an interpolated gradient"
            description = """
                Calculates an interpolated background image from several fix points and removes it.
                
                This script is useful for astrophotography if the fix points are chosen to represent the background.
                
                Use the --debug option to save intermediate images for manual analysis.
                """
            arguments {
                double("removePercent") {
                    description = """
                        The percentage of the calculated background that will be removed.
                        """
                    default = 100.0
                }
                int("gridSize") {
                    description = """
                        The size of the grid in the x and y axis.
                        The number of grid points is the square of the `gridSize`.
                        """
                    default = 5
                }
                double("kappa") {
                    description = """
                        The kappa factor is used in sigma-clipping of the grid to ignore grid points that do not contain enough background.
                        """
                    default = 0.5
                }
            }
            single {
                fun pointGrid(width: Int, height: Int, xCount: Int, yCount: Int): List<Pair<Int, Int>> {
                    val grid = mutableListOf<Pair<Int, Int>>()
                    for (x in 0 until xCount) {
                        for (y in 0 until yCount) {
                            val xCenter = (width.toDouble() / xCount * (x + 0.5)).toInt()
                            val yCenter = (height.toDouble() / yCount * (y + 0.5)).toInt()
                            grid.add(Pair(xCenter, yCenter))
                        }
                    }
                    return grid
                }

                fun sigmaClipPointGrid(image: Image, grid: List<Pair<Int, Int>>, kappa: Double = 0.5): List<Pair<Int, Int>> {
                    val gridWithMedian = grid.map {
                        val median = image.cropCenter(100, it.first, it.second).values().fastMedian()
                        Pair(it, median)
                    }
                    val gridMedian = gridWithMedian.map { it.second }.median()
                    val gridSigma = gridWithMedian.map { it.second }.stddev()

                    val low = gridMedian - gridSigma * kappa
                    val high = gridMedian + gridSigma * kappa

                    return gridWithMedian.filter { it.second in low..high } .map { it.first }
                }

                val removePercent: Double by arguments
                val gridSize: Int by arguments
                val kappa: Double by arguments

                val grid = pointGrid(inputImage.width, inputImage.height, gridSize, gridSize)
                val clippedGrid = sigmaClipPointGrid(inputImage, grid, kappa)
                if (verboseMode) {
                    println("The ${grid.size} points of the grid have been reduced to ${clippedGrid.size} by sigma-clipping")
                }

                var backgroundImage = inputImage.interpolate(clippedGrid)
                if (debugMode) {
                    val backgroundFile = inputFile.prefixName("background_")
                    println("Writing $backgroundFile")
                    ImageWriter.write(backgroundImage, backgroundFile)
                }

                if (verboseMode) println("Subtracting $removePercent% background glow from original image ...")
                backgroundImage = backgroundImage * (removePercent/100.0)

                if (debugMode) {
                    val deltaFile = inputFile.prefixName("delta_")
                    println("Saving $deltaFile for manual analysis")
                    val deltaImage = deltaChannel(inputImage, backgroundImage)
                    ImageWriter.write(deltaImage, deltaFile)
                }

                inputImage - backgroundImage
            }
        }


    fun scriptRemoveVignette(): Script =
        kimage(0.1) {
            name = "remove-vignette"
            description = """
                Removes the background from the input image by subtracting a gradient calculated from the color of fix points.
                
                This script is useful for astrophotography if the fix points are chosen to represent the background.
                
                Use the --debug option to save intermediate images for manual analysis.
                """
            arguments {
                double("removePercent") {
                    description = """
                        The percentage of the calculated background that will be removed.
                        """
                    default = 100.0
                }
                int("gridSize") {
                    description = """
                        The size of the grid in the x and y axis.
                        The number of grid points is the square of the `gridSize`.
                        """
                    default = 5
                }
                double("kappa") {
                    description = """
                        The kappa factor is used in sigma-clipping of the grid to ignore grid points that do not contain enough background.
                        """
                    default = 2.0
                }
            }
            single {
                fun Float.finiteOrElse(default: Float = 0f) = if (this.isFinite()) this else default

                val removePercent: Double by arguments
                val gridSize: Int by arguments
                val kappa: Double by arguments

                val centerX = inputImage.width / 2
                val centerY = inputImage.height / 2

                val calculatedMaxDistance = centerX + centerY // TODO calculate better
                val matrix = inputImage[Channel.Luminance]
                val distanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }
                val clippedDistanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }

                val totalMedian = matrix.fastMedian()
                val totalStddev = matrix.stddev()
                val low = totalMedian - totalStddev * kappa
                val high = totalMedian + totalStddev * kappa
                println("totalMedian = $totalMedian")
                println("totalStddev = $totalStddev")

                var maxDistance = 0
                var clippedMaxDistance = 0
                for (y in 0 until inputImage.height) {
                    for (x in 0 until inputImage.width) {
                        val value = matrix.getPixel(x, y)
                        val dx = (centerX-x).toDouble()
                        val dy = (centerY-y).toDouble()
                        val distance = sqrt(dx*dx + dy*dy).toInt()
                        distanceValues[distance].add(value.toFloat())
                        maxDistance = max(maxDistance, distance)
                        if (value in low..high) {
                            clippedDistanceValues[distance].add(value.toFloat())
                            clippedMaxDistance = max(maxDistance, distance)
                        }
                    }
                }

//                val reducedValues = FloatArray(maxDistance)
//                var lastMedian = distanceValues[maxDistance-1].toFloatArray().median()
//                val alpha = 0.01
//                for (i in maxDistance-1 downTo 0) {
//                    val low = lastMedian - alpha
//                    val high = lastMedian + alpha
//                    val value = distanceValues[i].toFloatArray().sigmaClip(low.toFloat(), high.toFloat(), keepLast = false).median()
//                    reducedValues[i] = if (value.isFinite()) value else 0.0f
//                    lastMedian = if (value.isFinite()) value else lastMedian
//                }

                val x = mutableListOf<Float>()
                val y = mutableListOf<Float>()
                for (i in 0 until clippedMaxDistance) {
                    val median = clippedDistanceValues[i].toFloatArray().medianInplace()
                    if (median.isFinite()) {
                        x.add(i.toFloat())
                        y.add(median)
                    }
                }

                val gaussSigma = 4000f
                val gaussAmplitude = 0.40
                fun gaussFunction(x: Float) = gaussAmplitude * exp(x*x/-2/(gaussSigma*gaussSigma))

                val cauchySigma = 5100f
                val cauchyAmplitude = 0.40
                fun cauchyFunction(x: Float) = cauchyAmplitude * 1f/(1+(x/cauchySigma).pow(2f))

                val polynomialFunction = polyRegression(x.toFloatArray(), y.toFloatArray())

                for (i in 0 until maxDistance) {
                    val count = distanceValues[i].size
                    val average = distanceValues[i].toFloatArray().average().finiteOrElse()
                    val median = distanceValues[i].toFloatArray().medianInplace().finiteOrElse()
                    //val sigmaWinsorizeInplace = distanceValues[i].toFloatArray().sigmaClip(0.01f, keepLast=false).medianInplace().finiteOrElse()
                    val regression = polynomialFunction(i.toFloat())
                    val gauss = gaussFunction(i.toFloat())
                    val cauchy = cauchyFunction(i.toFloat())
                    //println("  $i, $average, $median, ${sigmaWinsorizeInplace}, ${reducedValues[i]}, $regression")
                    println("  $i, $average, $median, $regression, $gauss, $cauchy")
                }

                val flatMatrix = CalculatedMatrix(inputImage.width, inputImage.height) { row, column ->
                    val dx = (centerX-column).toDouble()
                    val dy = (centerY-row).toDouble()
                    val distance = sqrt(dx*dx + dy*dy).toFloat()
                    cauchyFunction(distance)
                }

                var flatImage = MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to flatMatrix,
                    Channel.Green to flatMatrix,
                    Channel.Blue to flatMatrix)

                inputImage / flatImage * flatMatrix.max()
            }
        }

    // based on http://rosettacode.org/wiki/Polynomial_regression#Kotlin
    fun polyRegression(x: FloatArray, y: FloatArray): ((Float) -> Float) {
        val xm = x.average()
        val ym = y.average()
        val x2m = x.map { it * it }.average()
        val x3m = x.map { it * it * it }.average()
        val x4m = x.map { it * it * it * it }.average()
        val xym = x.zip(y).map { it.first * it.second }.average()
        val x2ym = x.zip(y).map { it.first * it.first * it.second }.average()

        val sxx = x2m - xm * xm
        val sxy = xym - xm * ym
        val sxx2 = x3m - xm * x2m
        val sx2x2 = x4m - x2m * x2m
        val sx2y = x2ym - x2m * ym

        val b = (sxy * sx2x2 - sx2y * sxx2) / (sxx * sx2x2 - sxx2 * sxx2)
        val c = (sx2y * sxx - sxy * sxx2) / (sxx * sx2x2 - sxx2 * sxx2)
        val a = ym - b * xm - c * x2m

        fun abc(xx: Float) = a + b * xx + c * xx * xx

        println("y = $a + ${b}x + ${c}x^2\n")
//        println(" Input  Approximation")
//        println(" x   y     y1")
//        for ((xi, yi) in x zip y) {
//            println("$xi, $yi, ${abc(xi)}")
//        }
        return ::abc
    }

    fun scriptTestMulti(): Script =
        kimage(0.1) {
            name = "test-multi"
            title = "Test script to show how to handle multiple images in a kimage script"
            description = """
                Example script as starting point for developers.
                """
            arguments {
                int("intArg") {
                    description = "Example argument for an int value."
                    min = 0
                    max = 100
                    default = 0
                }
                optionalInt("optionalIntArg") {
                    description = "Example argument for an optional int value."
                    min = 0
                    max = 100
                }
                double("doubleArg") {
                    description = "Example argument for a double value."
                    min = 0.0
                    max = 100.0
                    default = 50.0
                }
                boolean("booleanArg") {
                    description = "Example argument for a boolean value."
                    default = false
                }
                string("stringArg") {
                    description = "Example argument for a string value."
                    default = "undefined"
                }
                string("allowedStringArg") {
                    description = "Example argument for a string value with some allowed strings."
                    allowed = listOf("red", "green", "blue")
                    default = "red"
                }
                string("regexStringArg") {
                    description = "Example argument for a string value with regular expression."
                    regex = "a+"
                    default = "aaa"
                }
                list("listOfIntArg") {
                    description = "Example argument for a list of integer values."
                    min = 1

                    default = listOf(1, 2, 3)
                    int {
                        description = "A single integer value"
                        min = 0
                        max = 9
                    }
                }
                optionalList("optionalListOfIntArg") {
                    description = "Example argument for an optional list of integer values."
                    min = 1

                    int {
                        description = "A single integer value"
                        min = 0
                        max = 9
                    }
                }
                record("recordArg") {
                    description = "Example argument for a record containing different values."

                    int("recordInt") {
                        default = 2
                    }
                    string("recordString") {
                        default = "hello"
                    }
                    double("recordDouble") {
                        default = 3.14
                    }
                }
                optionalRecord("optionalRecordArg") {
                    description = "Example argument for an optional record containing different values."

                    int("optionalRecordInt") {
                    }
                    string("optionalRecordString") {
                    }
                    double("optionalRecordDouble") {
                    }
                }
            }

            multi {
                val intArg: Int by arguments
                val optionalIntArg: Optional<Int> by arguments
                val doubleArg: Double by arguments
                val booleanArg: Boolean by arguments
                val stringArg: String by arguments
                val allowedStringArg: String by arguments
                val regexStringArg: String by arguments
                val listOfIntArg: List<Int> by arguments
                val optionalListOfIntArg: Optional<List<Int>> by arguments
                val recordArg: Map<String, Any> by arguments
                val recordInt: Int by recordArg
                val recordString: String by recordArg
                val recordDouble: Double by recordArg

                println("Raw Arguments:")
                for (rawArgument in rawArguments) {
                    val key: String = rawArgument.key
                    val value: String = rawArgument.value
                    println("  Argument: ${key} = ${value}")
                }
                println()

                println("Processed Arguments:")
                println("  intArg = $intArg")
                println("  optionalIntArg = $optionalIntArg")
                println("  doubleArg = $doubleArg")
                println("  booleanArg = $booleanArg")
                println("  stringArg = $stringArg")
                println("  allowedStringArg = $allowedStringArg")
                println("  regexStringArg = $regexStringArg")
                println("  listOfIntArg = $listOfIntArg")
                println("  optionalListOfIntArg = $optionalListOfIntArg")
                println("  recordArg = $recordArg")
                println("  recordInt = $recordInt")
                println("  recordString = $recordString")
                println("  recordDouble = $recordDouble")

                println("Input Files:")
                for (file in inputFiles) {
                    println("  File: $file exists=${file.exists()}")
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