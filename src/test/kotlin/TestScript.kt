import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.astro.StarDetector
import ch.obermuhlner.kimage.awt.drawAwt
import ch.obermuhlner.kimage.fft.Complex
import ch.obermuhlner.kimage.fft.ComplexMatrix
import ch.obermuhlner.kimage.fft.FFT
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import ch.obermuhlner.util.StreamGobbler
import org.apache.commons.imaging.Imaging
import org.apache.commons.math3.fitting.*
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints

import java.io.*
import java.lang.Math.toRadians
import java.nio.file.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.*

object TestScript {

    @JvmStatic
    fun main(args: Array<String>) {
        KImageManager.addScriptDirectory(File("src/main/dist"))

        val orionImages = arrayOf(
            "images/align/orion1.png",
            "images/align/orion2.png",
            "images/align/orion3.png",
            "images/align/orion4.png",
            "images/align/orion5.png",
            "images/align/orion6.png"
        )
        val orion2Images = arrayOf("images/align/orion1.png", "images/align/orion2.png")
        val alignedOrionImages = arrayOf(
            "images/align/aligned_orion1.png",
            "images/align/aligned_orion2.png",
            "images/align/aligned_orion3.png",
            "images/align/aligned_orion4.png",
            "images/align/aligned_orion5.png",
            "images/align/aligned_orion6.png"
        )
        //val hdrImages = arrayOf("images/hdr/hdr1.jpg", "images/hdr/hdr2.jpg", "images/hdr/hdr3.jpg", "images/hdr/hdr4.jpg")
        val hdrImages = arrayOf(
            "images/hdr/HDRI_Sample_Scene_Window_-_01.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_02.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_03.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_04.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_05.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_06.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_07.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_08.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_09.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_10.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_11.jpg",
            "images/hdr/HDRI_Sample_Scene_Window_-_12.jpg"
        )

        //runScript(scriptAlign(), mapOf("centerX" to "447", "centerY" to "517", "checkRadius" to "20", "searchRadius" to "100", "medianRadius" to "0", "subPixelStep" to "0", "sort" to "false"), *orion2Images)
        //runScript(scriptAlign(), mapOf("sort" to "false"), *orion2Images)
        //runScript(scriptStackMax(), mapOf(), *orionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *alignedOrionImages)
        //runScript(scriptStack(), mapOf("kappa" to "2.0"), *orionImages)
        //runScript(scriptRemoveBackgroundMedian(),"images/align/orion1.png")
        //runScript(scriptHistogram(), "images/align/output_orion1.png")
        //runScript(scriptCalibrate())
        //runScript(scriptHDR(), mapOf(), *alignedOrionImages)
        //runScript(scriptHDR(), mapOf(), *hdrImages)
        //runScript(scriptCalibrate(), mapOf("biasDir" to "images/align"))
        //runScript(scriptCalibrate(), mapOf("biasDir" to "images/align"))

        //runScript(scriptRemoveVignette(), mapOf(), "images/vignette/flat_large.tif")
        //runScript(scriptRemoveVignette(), mapOf("mode" to "rgb"), "images/vignette/IMG_6800.TIF")
        runScript(scriptRemoveVignette(), mapOf("mode" to "rgb"), "images/vignette/debayer_MasterFlat_ISO100.tif")

        //runScript(scriptRemoveOutliers(), mapOf("kappa" to "10"), "images/outlier-pixels/bias_10s_ISO1600.tiff")
        //runScript(scriptRemoveOutliers(), mapOf("low" to "0", "high" to "0.5"), "images/outlier-pixels/bias_10s_ISO1600.tiff")
        //runScript(scriptRemoveOutliers(), mapOf("kappa" to "20"), "images/outlier-pixels/bias_1over4000s_ISO1600.TIF")

        //runScript(scriptTestMulti(), mapOf())

        //runScript(scriptConvertRaw(), mapOf("interpolation" to "none-unscaled"), "images/outlier-pixels/bias_10s_ISO1600.CR2")
        //runScript(scriptConvertRaw(), mapOf(), "images/raw/IMG_8920.cr2")

        // IMG_8922.CR2 : multipliers 2.122024 1.000000 1.455032 1.000000

        //runScript(scriptDebayer(), mapOf("interpolation" to "bilinear", "whitebalance" to "highlight-median", "localX" to "6036", "localY" to "2389", "localRadius" to "10", "stretch" to "true"), "images/raw/IMG_8922_pure-unscaled.tiff")

        //runScript(scriptDeconvolute(), mapOf(), "images/gauss3_animal.png")
        //runScript(scriptDeconvolute(), mapOf("method" to "fft", "psf" to "gauss3x3"), "images/gauss3_animal.png")

        //runScript(scriptSamplePSF(), mapOf("sampleX" to "20", "sampleY" to "20", "radius" to "3"), "images/gauss3_animal.png")

        //runScript(scriptWhitebalance(), mapOf("whitebalance" to "local", "localX" to "1897", "localY" to "3207"), "images/colorchart/debayer_colorchart_cloudy.tiff")
        //runScript(scriptWhitebalance(), mapOf(), "images/M31.tif")

        //runScript(scriptComposition(), mapOf(), "images/animal.png")

        //runScript(scriptPickBest(), mapOf("centerX" to "400", "centerY" to "400", "radius" to "200"), *alignedOrionImages)

        //runScript(scriptSharpen(), mapOf(), "images/sharpen/moon.tif")
        //runScript(scriptFindDirt(), mapOf(), "images/dirt/IMG_3194.JPG")

        //runScript(scriptTransformSaturationBrightness(), mapOf(), "images/animal.png")

        //runScript(scriptColorStretchCurve2(), mapOf(), "images/animal.png")

        //runScript(scriptStatistics(), mapOf(), "images/animal.png")

        //runScript(scriptFindStars(), mapOf(), "images/find_stars/M46_M47_stacked.png")
        //runScript(scriptFindStars(), mapOf(), "images/find_stars/M46_M47_stacked_small.png")
        //runScript(scriptFindStars(), mapOf(), "images/align/orion1.png")

        //runScript(scriptRemoveBackgroundGradient(), mapOf(), "images/find_stars/M46_M47_stacked.png")
    }

    private fun scriptValueMask(): Script =
        kimage(0.1) {
            name = "value-mask"
            title = "Create a value range mask."
            description = """
                Create a mask image based on a value range.
                """
            arguments {
                double("low") {
                    min = 0.0
                    max = 1.0
                    default = 0.0
                }
                double("high") {
                    min = 0.0
                    max = 1.0
                    default = 1.0
                }
                int("blur") {
                    min = 0
                    default = 0
                }
                boolean("invert") {
                    default = false
                }
            }

            single {
                val low: Double by arguments
                val high: Double by arguments
                val blur: Int by arguments
                val invert: Boolean by arguments

                var matrix = inputImage[Channel.Gray]
                matrix.onEach { _, _, value ->
                    if (value < low) {
                        0.0
                    } else if (value > high) {
                        0.0
                    } else {
                        value
                    }
                }

                if (blur > 0) {
                    matrix = matrix.gaussianBlurFilter(blur)
                }
                if (invert) {
                    matrix.onEach { _, _, value ->
                        1.0 - value
                    }
                }

                MatrixImage(matrix)
            }
        }

    private fun scriptFindStars(): Script =
        kimage(0.1) {
            name = "find-stars"
            title = "Find stars."
            description = """
                Finds stars in an image.
                """
            arguments {
            }

            single {
                var matrix = inputImage[Channel.Gray]
                val detector = StarDetector(matrix)
                val stars = detector.detectPotentialStars()
                println("Found ${stars.size} stars")

                val analyzedStars = detector.analyzePotentialStars(stars)

                if (debugMode) {
                    val red = matrix.copy()
                    val green = matrix.copy()
                    val blue = matrix.copy()
                    analyzedStars.forEach {
                        val starRadius = it.radius.toInt()
                        val x = (it.x + 0.5).toInt()
                        val y = (it.y + 0.5).toInt()
                        if (it.valid) {
                            for (dy in -starRadius .. starRadius) {
                                for (dx in -starRadius .. starRadius) {
                                    if (dx*dx+dy*dy < starRadius*starRadius) {
                                        blue[x+dx, y+dy] = 1.0
                                    }
                                }
                            }
                        }
                        red[x, y] = if (it.valid) 0.0 else 1.0
                        green[x, y] = if (it.valid) 1.0 else 0.0
                        blue[x, y] = 0.0
                    }
                    val image = MatrixImage(red, green, blue)

                    val starDebugFile = inputFile.prefixName(outputDirectory, "star_debug_")
                    println("Saving $starDebugFile for manual analysis")
                    ImageWriter.write(image, starDebugFile)
                    println()
                }


                val red = DoubleMatrix(matrix.width, matrix.height)
                val green = DoubleMatrix(matrix.width, matrix.height)
                val blue = DoubleMatrix(matrix.width, matrix.height)
                analyzedStars.forEach {
                    val starRadius = it.radius.toInt()
                    val x = (it.x + 0.5).toInt()
                    val y = (it.y + 0.5).toInt()
                    if (it.valid) {
                        for (dy in -starRadius .. starRadius) {
                            for (dx in -starRadius .. starRadius) {
                                if (dx*dx+dy*dy < starRadius*starRadius) {
                                    red[x+dx, y+dy] = 1.0
                                    green[x+dx, y+dy] = 1.0
                                    blue[x+dx, y+dy] = 1.0
                                }
                            }
                        }
                    }
                }
                MatrixImage(red, green, blue)
            }
        }


    private fun scriptFindStarsPOC(): Script =
        kimage(0.1) {
            name = "find-stars"
            title = "Find stars."
            description = """
                Finds stars in an image.
                """
            arguments {
            }

            single {
                val medianRadius = 0
                val blurStartRadius = 1
                val blurCount = 4

                var matrix = inputImage[Channel.Gray]
                if (medianRadius > 0) {
                    matrix = matrix.medianFilter(medianRadius)
                }
                val median = matrix.median()
                matrix = matrix - DoubleMatrix(matrix.width, matrix.height) { _, _ ->
                    median
                }

                val diffMatrices = mutableListOf<Matrix>()
                val diffRadii = mutableListOf<Int>()
                val diffFactors = mutableListOf<Int>()

                var lastBlurMatrix = matrix
                var blurRadius = blurStartRadius
                var diffFactor = 1
                for (blurIndex in 0 until blurCount) {
                    val scaleFactor = 1.0 / diffFactor.toDouble()
                    val blurMatrix = matrix.gaussianBlurFilter(blurRadius).scaleBy(scaleFactor, scaleFactor)
                    val diffMatrix = lastBlurMatrix - blurMatrix
                    ImageWriter.write(MatrixImage(blurMatrix), File("blur_$blurIndex.png"))
                    ImageWriter.write(MatrixImage(diffMatrix), File("diff_$blurIndex.png"))

                    diffMatrices += diffMatrix
                    diffRadii += blurRadius
                    diffFactors += diffFactor
                    blurRadius *= 2
                    diffFactor *= 2
                    lastBlurMatrix = blurMatrix.scaleBy(0.5, 0.5)
                }

                val stars = DoubleMatrix(matrix.width, matrix.height)

                for (diffIndex in diffMatrices.indices.reversed()) {
                    var max = -Double.MAX_VALUE
                    var maxIndex = 0
                    var maxX = 0
                    var maxY = 0
                    diffMatrices[diffIndex].forEach { x, y, value ->
                        if (value > max) {
                            max = value
                            maxX = x * diffFactors[diffIndex]
                            maxY = y * diffFactors[diffIndex]
                            maxIndex = diffIndex
                        }
                    }

                    val lowerLimit = 0.1 // max * 0.1

                    while (max > lowerLimit) {
                        val starRadius = diffRadii[maxIndex]
                        println("STAR in matrix index $maxIndex radius $starRadius at $maxX, $maxY with value $max")

                        for (dy in -starRadius .. starRadius) {
                            for (dx in -starRadius .. starRadius) {
                                if (dx*dx+dy*dy < starRadius*starRadius) {
                                    for (diffIndex in diffMatrices.indices) {
                                        diffMatrices[diffIndex][(maxX+dx) / diffFactors[diffIndex], (maxY+dy) / diffFactors[diffIndex]] = 0.0
                                    }
                                    stars[maxX+dx, maxY+dy] = 1.0
                                }
                            }
                        }

                        //ImageWriter.write(MatrixImage(stars), File("stars$debugIndex.png"))
                        //debugIndex++

                        max = -Double.MAX_VALUE
                        diffMatrices[diffIndex].forEach { x, y, value ->
                            if (value > max) {
                                max = value
                                maxX = x * diffFactors[diffIndex]
                                maxY = y * diffFactors[diffIndex]
                                maxIndex = diffIndex
                            }
                        }

                        ImageWriter.write(MatrixImage(diffMatrices[diffIndex]), File("cleaned_diff_$diffIndex.png"))
                    }
                }

                MatrixImage(stars)
            }
        }

    private fun scriptShowMinClamped(): Script =
        kimage(0.1) {
            name = "show-min-clamped"
            title = "Show all pixels with value 0."
            description = """
                Creates a false color image showing all pixels with a value of 0.
                """
            arguments {
            }

            single {
                val channelMatrices = mutableMapOf<Channel, Matrix>()
                for (channel in inputImage.channels) {
                    val matrix = inputImage[channel].copy()
                    matrix.onEach { v ->
                        if (v <= 0.0) {
                            1.0
                        } else {
                            0.0
                        }
                    }
                    channelMatrices[channel] = matrix
                }

                MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ ->
                    channelMatrices[channel]!!
                }
            }
        }

    private fun scriptStatistics(): Script =
        kimage(0.1) {
            name = "statistics"
            title = "Print statistical info about images"
            arguments {
            }

            multi {
                println(String.format("%-40s %8s %8s %8s %8s %8s %8s %8s", "Name", "Low#", "High#", "Min", "Max", "Median", "Stddev", "NormStddev"))
                for (file in inputFiles) {
                    print(String.format("%-40s ", file.name))
                    if (file.isFile) {
                        try {
                            val image = ImageReader.read(file)

                            val clampLow = image.values().count { v -> v <= 0.0 }
                            val clampHigh = image.values().count { v -> v >= 1.0 }
                            val min = image.values().min()
                            val max = image.values().max()
                            val median = image.values().median()
                            val stddev = image.values().stddev()

                            val normalizedImage = image / (median / 0.5)
                            val normalizedStddev = normalizedImage.values().stddev()

                            print(
                                String.format(
                                    "%8d %8d %8.5f %8.5f %8.5f %8.5f %8.5f",
                                    clampLow,
                                    clampHigh,
                                    min,
                                    max,
                                    median,
                                    stddev,
                                    normalizedStddev
                                )
                            )
                        } catch (ex: Exception) {
                            // ignore
                        }
                    }
                    println()
                }
            }
        }

    private fun scriptRenameFile(): Script =
        kimage(0.1) {
            name = "rename-file"
            title = "Copy and rename files into another directory"
            description = """
                Rename images.
                """
            arguments {
                file("target") {
                    description = """
                        The target directory to move the image file into.
                        """
                    isDirectory = true
                }
                boolean("keepExtension") {
                    default = true
                }
            }

            multi {
                val target: File by arguments
                val keepExtension: Boolean by arguments

                if (!target.exists()) {
                    target.mkdirs()
                }

                fun expandFileName(fileName: String, varName: String, expander : () -> String): String {
                    val varNameToReplace = "{$varName}"
                    return if (fileName.contains(varNameToReplace)) {
                        fileName.replace(varNameToReplace, expander())
                    } else {
                        fileName
                    }
                }

                for (inputFile in inputFiles) {
                    println("Moving $inputFile into $target directory")
                    var name = if(keepExtension) inputFile.nameWithoutExtension else inputFile.name
                    val extension = inputFile.extension

                    name = expandFileName(name, "name") {
                        inputFile.nameWithoutExtension
                    }
                    name = expandFileName(name, "extension") {
                        inputFile.extension
                    }

                    val metadata = Imaging.getMetadata(inputFile)
                    if (metadata != null) {
                    }

                    val newName = if(keepExtension) "$name.$extension" else name
                    Files.move(inputFile.toPath(), File(target, newName).toPath())
                }
            }
        }


    private fun scriptColorStretchCurve2(): Script =
        kimage(0.1) {
            name = "color-stretch-curve"
            title = "Stretch the colors non-linearly to fill the entire value range"
            description = """
                The colors are first brightened using a power function and then a curve is applied.
                
                The idea for this script is based on https://clarkvision.com/articles/astrophotography-rnc-color-stretch/
                """
            arguments {
                double("brightness") {
                    description = """
                        The power value of the brightness increase.
                        
                        - A power value > 1 increases the brightness.
                        - A power value = 0 does not change the brightness.
                        - A power value < 1 increases the brightness.
                        """
                    min = 0.0
                    default = 2.0
                }
                list("curve") {
                    description = """
                        The curve shape used to modify the contrast.
                        """
                    point {
                        min = Point(0.0, 0.0)
                        max = Point(1.0, 1.0)
                    }
                    hint = Hint.Curve
                    default = listOf(Point(0.0, 0.0), Point(0.2, 0.1), Point(0.8, 0.9), Point(1.0, 1.0))
                }
            }

            single {
                val brightness: Double by arguments
                val curve: List<Point> by arguments

                val histogramWidth = 256
                val histogramHeight = 150

                val (power1, power2) = if (brightness < 1000.0) {
                    Pair(brightness, 1.0)
                } else {
                    Pair(brightness.pow(1.0 / 5.0), 5.0)
                }

                var image = inputImage

                if (debugMode) {
                    println("Input image - average: ${image.values().average()}")
                    println("Input image - median: ${image.values().fastMedian()}")
                    println("Input image - stddev: ${image.values().stddev()}")

                    val histogramInputFile = inputFile.prefixName(outputDirectory, "hist_input_")
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
                    println("After brightness correction - average: ${image.values().average()}")
                    println("After brightness correction - median: ${image.values().fastMedian()}")
                    println("After brightness correction - stddev: ${image.values().stddev()}")

                    val histogramBrightnessFile = inputFile.prefixName(outputDirectory, "hist_brightness_")
                    println("Saving $histogramBrightnessFile (after brightness correction) for manual analysis")
                    ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramBrightnessFile)
                    println()
                }

                val curvePointsX = mutableListOf<Double>()
                val curvePointsY = mutableListOf<Double>()

                for (point in curve) {
                    curvePointsX.add(point.x)
                    curvePointsY.add(point.y)
                }

                println("Curve Points:")
                println("  X: $curvePointsX")
                println("  Y: $curvePointsY")
                println()

                val spline: SplineInterpolator = SplineInterpolator.createMonotoneCubicSpline(curvePointsX, curvePointsY)

                image = image.onEach { v -> spline.interpolate(v) }

                if (debugMode) {
                    println("After curve correction - average: ${image.values().average()}")
                    println("After curve correction - median: ${image.values().fastMedian()}")
                    println("After curve correction - stddev: ${image.values().stddev()}")

                    val histogramOutputFile = inputFile.prefixName(outputDirectory, "hist_output_")
                    println("Saving $histogramOutputFile (after curve correction) for manual analysis")
                    ImageWriter.write(image.histogramImage(histogramWidth, histogramHeight), histogramOutputFile)
                    println()
                }

                image
            }
        }

    private fun scriptTransformSaturationBrightness(): Script =
        kimage(0.1) {
            name = "transform-saturation-brightness"
            title = "Transform the saturation and brightness of an image"
            description = """
                Transform the saturation and brightness of an image.
                """
            arguments {
                string("saturationFunction") {
                    allowed = listOf("linear", "power", "exaggerate")
                    default = "linear"
                }
                double("saturationFactor") {
                    default = 1.0
                }
                string("brightnessFunction") {
                    allowed = listOf("linear", "power", "exaggerate")
                    default = "linear"
                }
                double("brightnessFactor") {
                    default = 1.0
                }
            }

            single {
                val saturationFunction: String by arguments
                val saturationFactor: Double by arguments
                val brightnessFunction: String by arguments
                val brightnessFactor: Double by arguments

                val hue = inputImage[Channel.Hue]
                val saturation = inputImage[Channel.Saturation]
                val brightness = inputImage[Channel.Brightness]

                val changedSaturation = when(saturationFunction) {
                    "linear" -> saturation * saturationFactor
                    "power" -> saturation.copy().onEach { v -> v.pow(1.0/saturationFactor) }
                    "exaggerate" -> saturation.copy().onEach { v -> exaggerate(v * saturationFactor) }
                    else -> throw IllegalArgumentException("Unknown function: $saturationFunction")
                }
                val changedBrightness = when(brightnessFunction) {
                    "linear" -> brightness * brightnessFactor
                    "power" -> brightness.copy().onEach { v -> v.pow(1.0/brightnessFactor) }
                    "exaggerate" -> brightness.copy().onEach { v -> exaggerate(v * brightnessFactor) }
                    else -> throw IllegalArgumentException("Unknown function: $brightnessFunction")
                }

                val hsbImage = MatrixImage(inputImage.width, inputImage.height,
                    Channel.Hue to hue,
                    Channel.Saturation to changedSaturation,
                    Channel.Brightness to changedBrightness)

                MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to hsbImage[Channel.Red],
                    Channel.Green to hsbImage[Channel.Green],
                    Channel.Blue to hsbImage[Channel.Blue])
            }
        }

    private fun scriptFilter(): Script =
        kimage(0.1) {
            name = "filter"
            title = "Filter an image"
            description = """
                Filter an image according the specified arguments.
                """
            arguments {
                string("filter") {
                    description = """
                The filter algorithm.
                
                - `blur` uses a gaussian blur filter of the specified `radius`
                - `unsharpMask` uses an unsharp mask of the specified `radius`
                - `blur3x3` uses a gaussian blur filter using a 3x3 kernel
                - `blur5x5` uses a gaussian blur filter using a 5x5 kernel
                - `blur7x7` uses a gaussian blur filter using a 7x7 kernel
                - `median` uses a median filter of the specified `radius`
                - `average` uses an average filter of the specified `radius`
                - `sharpen3x3` uses a 3x3 sharpen mask
                - `unsharpMask3x3` uses a 3x3 unsharp mask
                - `edgeDetectionStrong3x3` detects edges along the horizontal/vertial axes and both diagonals using a 3x3 kernel
                - `edgeDetectionCross3x3` detects edges along the horizontal/vertial axes using a 3x3 kernel
                - `edgeDetectionDiagonal3x3` detects edges along both diagonals using a 3x3 kernel
                """
                    allowed = listOf("blur", "median", "average", "unsharpMask", "sobel3x3", "sobel5x5", "blur3x3", "blur5x5", "blur7x7", "sharpen3x3", "unsharpMask3x3", "edgeDetectionStrong3x3", "edgeDetectionCross3x3", "edgeDetectionDiagonal3x3")
                }
                int("radius") {
                    description = """
                The radius in pixels for the `blur`, 'unsharpMask', `median` and `average` filters.
                """
                    enabledWhen = Reference("filter").isEqual("blur", "median", "average", "unsharpMask")
                    min = 1
                    default = 3
                }
                double("strength") {
                    description = """
                The strength of the filter.
                """
                    enabledWhen = Reference("filter").isEqual("unsharpMask")
                    min = 0.0
                    max = 1.0
                    default = 0.5
                }
                string("channels") {
                    allowed = listOf("rgb", "red", "green", "blue", "hue", "saturation", "brightness", "hsb", "hs", "hb", "sb")
                    default = "rgb"
                }
            }

            single {
                val filter: String by arguments
                val radius: Int by arguments
                val strength: Double by arguments
                val channels: String by arguments

                fun matrixFilter(matrix: Matrix): Matrix {
                    return when (filter) {
                        "blur" -> matrix.gaussianBlurFilter(radius)
                        "median" -> matrix.medianFilter(radius)
                        "average" -> matrix.averageFilter(radius)
                        "sharpen" -> matrix.sharpenFilter()
                        "unsharpMask" -> matrix.unsharpMaskFilter(radius, strength)
                        "unsharpMask3x3" -> matrix.convolute(KernelFilter.UnsharpMask)
                        "blur3x3" -> matrix.convolute(KernelFilter.GaussianBlur3)
                        "blur5x5" -> matrix.convolute(KernelFilter.GaussianBlur5)
                        "blur7x7" -> matrix.convolute(KernelFilter.GaussianBlur7)
                        "edgeDetectionStrong3x3" -> matrix.edgeDetectionStrongFilter()
                        "edgeDetectionCross3x3" -> matrix.edgeDetectionCrossFilter()
                        "edgeDetectionDiagonal3x3" -> matrix.edgeDetectionDiagonalFilter()
                        "sobel3x3" -> matrix.sobelFilter3()
                        "sobel5x5" -> matrix.sobelFilter5()
                        else -> throw IllegalArgumentException("Unknown filter: $filter")
                    }
                }

                when(channels) {
                    "rgb", "red", "green", "blue" -> {
                        var red = inputImage[Channel.Red]
                        var green = inputImage[Channel.Green]
                        var blue = inputImage[Channel.Blue]

                        when(channels) {
                            "red" -> red = matrixFilter(red)
                            "green" -> green = matrixFilter(green)
                            "blue" -> blue = matrixFilter(blue)
                            "rgb" -> {
                                red = matrixFilter(red)
                                green = matrixFilter(green)
                                blue = matrixFilter(blue)
                            }
                            else -> throw IllegalArgumentException("Unknown channels: $channels")
                        }

                        MatrixImage(inputImage.width, inputImage.height,
                            Channel.Red to red,
                            Channel.Green to green,
                            Channel.Blue to blue)
                    }
                    else -> {
                        var hue = inputImage[Channel.Hue]
                        var saturation = inputImage[Channel.Saturation]
                        var brightness = inputImage[Channel.Brightness]

                        when(channels) {
                            "hue" -> hue = matrixFilter(hue)
                            "saturation" -> saturation = matrixFilter(saturation)
                            "brightness" -> brightness = matrixFilter(brightness)
                            "hsb" -> {
                                hue = matrixFilter(hue)
                                saturation = matrixFilter(saturation)
                                brightness = matrixFilter(brightness)
                            }
                            "hs" -> {
                                hue = matrixFilter(hue)
                                saturation = matrixFilter(saturation)
                            }
                            "hb" -> {
                                hue = matrixFilter(hue)
                                brightness = matrixFilter(brightness)
                            }
                            "sb" -> {
                                saturation = matrixFilter(saturation)
                                brightness = matrixFilter(brightness)
                            }
                            else -> throw IllegalArgumentException("Unknown channels: $channels")
                        }

                        val hsbImage = MatrixImage(inputImage.width, inputImage.height,
                            Channel.Hue to hue,
                            Channel.Saturation to saturation,
                            Channel.Brightness to brightness)

                        MatrixImage(inputImage.width, inputImage.height,
                            Channel.Red to hsbImage[Channel.Red],
                            Channel.Green to hsbImage[Channel.Green],
                            Channel.Blue to hsbImage[Channel.Blue])
                    }
                }
            }
        }


    private fun scriptFindDirt(): Script =
        kimage(0.1) {
            name = "find-dirt"
            title = "Find dirt on the sensor in a flat image"
            description = """
                Find dirt on the sensor in a flat image by creating a false color image showing the distance to the median.
                """
            arguments {
                string("channel") {
                    description = """
                        """
                    allowed = listOf("gray", "luminance", "red", "green", "blue")
                    default = "gray"
                }
                double("factor") {
                    default = 2.0
                }
            }

            single {
                val channel: String by arguments
                val factor: Double by arguments

                val measureChannel = when (channel) {
                    "gray" -> Channel.Gray
                    "luminance" -> Channel.Luminance
                    "red" -> Channel.Red
                    "green" -> Channel.Green
                    "blue" -> Channel.Blue
                    else -> throw IllegalArgumentException("Unknown channel: $channel")
                }

                val measureMatrix = inputImage[measureChannel]

                val median = measureMatrix.median();

                val f = 0.2
                var delta = Matrix.matrixOf(measureMatrix.width, measureMatrix.height) { x, y ->
                    (measureMatrix[x, y] - median)
                }
                val min = delta.min()
                val max = delta.max()
                delta.onEach { v ->
                    (v + min)/(max - min) - min / (max - min)
                }
                delta.onEach { v ->
                    if (v >= 0.0) {
                        v.pow(1.0 / factor)
                    } else {
                        -((-v).pow(1.0 / factor))
                    }
                }

                val red = delta.copy().onEach   { v -> if (v < 0.0) -v     else v * f }
                val green = delta.copy().onEach { v -> if (v < 0.0) -v * f else v * f }
                val blue = delta.copy().onEach  { v -> if (v < 0.0) -v * f else v     }


                MatrixImage(measureMatrix.width, measureMatrix.height,
                    Channel.Red to red,
                    Channel.Green to green,
                    Channel.Blue to blue
                )
            }
        }

    private fun scriptSharpen(): Script =
        kimage(0.1) {
            name = "sharpen"
            title = "Sharpen an image"
            description = """
                Sharpens an image using unsharp mask.
                """
            arguments {
                int("radius") {
                    description = """
                The radius in pixels for the unsharp mask filter.
                """
                    min = 0
                    default = 3
                }
                double("strength") {
                    description = """
                The strength of the unsharp mask filter.
                """
                    min = 0.0
                    default = 1.0
                }
                boolean("edgeDetection") {
                    default = true
                }
                int("edgePreBlurRadius") {
                    default = 1
                }
                string("edgeAlgorithm") {
                    allowed = listOf("sobel5x5", "sobel3x3", "edgeDetectionStrong", "edgeDetectionCross", "edgeDetectionDiagonal")
                    default = "sobel5x5"
                }
                boolean("edgeNormalize") {
                    default = true
                }
                int("edgePostBlurRadius") {
                    default = 0
                }
                double("edgeSensitivity") {
                    min = 0.0
                    max = 1.0
                    default = 0.5
                }
            }

            fun Matrix.unsharpMaskEdgeFilter(radius: Int, strength: Double, edge: Matrix): Matrix {
                val blurred = this.gaussianBlurFilter(radius)

                val m = (this - blurred) * strength
                val m2 = m elementTimes edge
                return this + m2
            }
            fun Image.unsharpMaskEdgeFilter(radius: Int, strength: Double, edge: Matrix): Image = MatrixImageFilter({ _, matrix -> matrix.unsharpMaskEdgeFilter(radius, strength, edge) }).filter(this)

            single {
                val radius: Int by arguments
                val strength: Double by arguments
                val edgeDetection: Boolean by arguments
                val edgePreBlurRadius: Int by arguments
                val edgeAlgorithm: String by arguments
                val edgePostBlurRadius: Int by arguments
                val edgeNormalize: Boolean by arguments
                val edgeSensitivity: Double by arguments

                var edgeMatrix: Matrix? = null
                if (edgeDetection) {
                    edgeMatrix = inputImage[Channel.Luminance]
                    edgeMatrix = edgeMatrix.gaussianBlurFilter(edgePreBlurRadius)
                    edgeMatrix = when (edgeAlgorithm) {
                        "sobel5x5" -> edgeMatrix.sobelFilter5()
                        "sobel3x3" -> edgeMatrix.sobelFilter3()
                        "edgeDetectionStrong" -> edgeMatrix.edgeDetectionStrongFilter()
                        "edgeDetectionCross" -> edgeMatrix.edgeDetectionCrossFilter()
                        "edgeDetectionDiagonal" -> edgeMatrix.edgeDetectionDiagonalFilter()
                        else -> throw IllegalArgumentException("Unknown edgeAlgorithm: $edgeAlgorithm")
                    }
                    edgeMatrix = edgeMatrix.gaussianBlurFilter(edgePostBlurRadius)
                    if (edgeNormalize) {
                        edgeMatrix = edgeMatrix / edgeMatrix.max()
                    }
                    edgeMatrix.onEach { v -> clamp(v, 0.0, 1.0) * edgeSensitivity + (1.0 - edgeSensitivity ) }

                    if (debugMode) {
                        val edgeDebugFile = inputFile.prefixName(outputDirectory, "edge_")
                        println("Saving $edgeDebugFile for manual analysis")
                        val edgeDebugImage = MatrixImage(edgeMatrix.width, edgeMatrix.height, Channel.Red to edgeMatrix, Channel.Green to edgeMatrix, Channel.Blue to edgeMatrix)
                        ImageWriter.write(edgeDebugImage, edgeDebugFile)
                        println()
                    }
                }

                if (edgeMatrix != null) {
                    inputImage.unsharpMaskEdgeFilter(radius, strength, edgeMatrix)
                } else {
                    inputImage.unsharpMaskFilter(radius, strength)
                }
            }
        }

    private fun scriptConvertRaw(): Script =
        kimage(0.1) {
            name = "convert-raw"
            title = "Convert an image from raw format into tiff"
            description = """
                Convert an image from raw format into tiff.
                """
            arguments {
                string("dcraw") {
                    description = """
               The `dcraw` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
                    default = "dcraw"
                }
                string("rotate") {
                    description = """
               The angle to rotate the image.
               - `auto` will use the information in the image rotate. 
               
              This corresponds to the `-t` option in the `dcraw` command line tool.
            """
                    allowed = listOf("0", "90", "180", "270", "auto")
                    default = "auto"
                }
                boolean("aspectRatio") {
                    description = """
               
              This corresponds to the `-j` option in the `dcraw` command line tool.
            """
                    default = true
                }
                string("whitebalance") {
                    description = """
              The whitebalance setting used to adjust the colors.
              
              - `camera` will use the whitebalance settings measured by the camera (if available)
              - `image` will calculate the whitebalance settings from the image
              - `local` will calculate the whitebalance settings from a local area of the image
              - `custom` will use the provided custom multipliers
              - `fixed` will use fixed default white balance multipliers.
              
              The `camera` whitebalance corresponds to the `-w` option in the `dcraw` command line tool.
              The `image` whitebalance corresponds to the `-a` option in the `dcraw` command line tool.
              The `custom` whitebalance corresponds to the `-r` option in the `dcraw` command line tool.
              The `fixed` whitebalance corresponds to the `-W` option in the `dcraw` command line tool.
            """
                    allowed = listOf("camera", "image", "local", "custom", "fixed")
                    default = "camera"
                }
                optionalInt("localX") {
                    hint = Hint.ImageX
                    enabledWhen = Reference("whitebalance").isEqual("local")
                }
                optionalInt("localY") {
                    hint = Hint.ImageY
                    enabledWhen = Reference("whitebalance").isEqual("local")
                }
                optionalInt("localRadius") {
                    enabledWhen = Reference("whitebalance").isEqual("local")
                    default = 10
                }
                optionalList("multipliers") {
                    description = """
              The four multipliers used for `custom` whitebalance mode.
              
              Corresponds to the `-r` option in the `dcraw` command line tool.
              """
                    min = 4
                    max = 4
                    enabledWhen = Reference("whitebalance").isEqual("custom")
                    double {
                    }
                }
                optionalInt("darkness") {
                    description = """
              The darkness level.
              
              Corresponds to the `-k` option in the `dcraw` command line tool.
              """
                    min = 0
                }
                optionalInt("saturation") {
                    description = """
              The saturation level.
              
              Corresponds to the `-S` option in the `dcraw` command line tool.
              """
                    min = 0
                }
                string("colorspace") {
                    description = """
                The colorspace to be used for the output image.
                """
                    allowed = listOf("raw", "sRGB", "AdobeRGB", "WideGamutRGB", "KodakProPhotoRGB", "XYZ", "ACES", "embed")
                    default = "sRGB"
                }
                string("interpolation") {
                    description = """
                The demosaicing interpolation method to use.
                
                - `bilinear`: Bilinear interpolation between neighboring pixels of the same color.
                
                  Corresponds to the `-q 0` option in the `dcraw` command line tool.
                - `VNG`: Variable Number Gradients
                
                  Corresponds to the `-q 1` option in the `dcraw` command line tool.
                - `PPG`: Patterned Pixel Grouping
                
                  Corresponds to the `-q 2` option in the `dcraw` command line tool.
                - `AHD`: Adaptive Homogeneity Directed
                
                  Corresponds to the `-q 3` option in the `dcraw` command line tool.
                - `none`: No interpolation, with automatic scaling to fill the value range.
                
                  Corresponds to the `-d` option in the `dcraw` command line tool.
                - `none-unscaled`: No interpolation, no scaling.
                
                  Corresponds to the `-D` option in the `dcraw` command line tool.
                - `none-uncropped`: No interpolation, no scaling, no cropping.
                
                  Corresponds to the `-E` option in the `dcraw` command line tool.
                """
                    allowed = listOf("bilinear", "VNG", "PPG", "AHD", "none", "none-unscaled", "none-uncropped")
                    default = "AHD"
                }
                int("medianPasses") {
                    description = """
                The number of 3x3 median passes to post-process the output image.
                
                Corresponds to the `-m` option in the `dcraw` command line tool.
                """
                    default = 0
                }
                string("bits") {
                    description = """
                The number of bits used to store a single value in the image.                
                
                The 16 bit mode corresponds to the `-6` option in the `dcraw` command line tool.
                """
                    allowed = listOf("8", "16")
                    default = "16"
                }
//        record("gamma") {
//            double("gammaPower") {
//                default = 2.222
//            }
//            double("gammaSlope") {
//                default = 4.5
//            }
//        }
                double("brightness") {
                    default = 1.0
                }
            }

            fun dcraw(
                dcraw: String,
                aspectRatio: Boolean,
                rotate: String,
                whitebalance: String,
                localX: Optional<Int>,
                localY: Optional<Int>,
                localRadius: Optional<Int>,
                multipliers: Optional<List<Double>>,
                darkness: Optional<Int>,
                saturation: Optional<Int>,
                colorspace: String,
                interpolation: String,
                medianPasses: Int,
                bits: String,
                brightness: Double,
                file: File,
                outputDirectory: File
            ) {
                val processBuilder = ProcessBuilder()

                val command = mutableListOf(dcraw, "-T", "-v")
                if (!aspectRatio) {
                    command.add("-j")
                }
                when (rotate) {
                    "0" -> {
                        command.add("-t")
                        command.add("0")
                    }
                    "90" -> {
                        command.add("-t")
                        command.add("90")
                    }
                    "180" -> {
                        command.add("-t")
                        command.add("180")
                    }
                    "270" -> {
                        command.add("-t")
                        command.add("270")
                    }
                    "auto" -> {}
                    else -> throw java.lang.IllegalArgumentException("Unknown rotate: $rotate")
                }
                when (whitebalance) {
                    "camera" -> command.add("-w")
                    "image" -> command.add("-a")
                    "local" -> {
                        command.add("-A")
                        command.add((localX.get() - localRadius.get()).toString())
                        command.add((localY.get() - localRadius.get()).toString())
                        command.add((localRadius.get() * 2 + 1).toString())
                        command.add((localRadius.get() * 2 + 1).toString())

                    }
                    "custom" -> {
                        command.add("-r")
                        if (multipliers.isPresent) {
                            command.add(multipliers.get()[0].toString())
                            command.add(multipliers.get()[1].toString())
                            command.add(multipliers.get()[2].toString())
                            command.add(multipliers.get()[3].toString())
                        } else {
                            command.add("1")
                            command.add("1")
                            command.add("1")
                            command.add("1")
                        }
                    }
                    "fixed" -> command.add("-W")
                    else -> throw java.lang.IllegalArgumentException("Unknown whitebalance: $whitebalance")
                }
                if (darkness.isPresent) {
                    command.add("-k");
                    command.add(darkness.get().toString());
                }
                if (saturation.isPresent) {
                    command.add("-S");
                    command.add(saturation.get().toString());
                }
                when (colorspace) {
                    "raw" -> {
                        command.add("-o")
                        command.add("0")
                    }
                    "sRGB" -> {
                        command.add("-o")
                        command.add("1")
                    }
                    "AdobeRGB" -> {
                        command.add("-o")
                        command.add("2")
                    }
                    "WideGamutRGB" -> {
                        command.add("-o")
                        command.add("3")
                    }
                    "KodakProPhotoRGB" -> {
                        command.add("-o")
                        command.add("4")
                    }
                    "XYZ" -> {
                        command.add("-o")
                        command.add("5")
                    }
                    "ACES" -> {
                        command.add("-o")
                        command.add("6")
                    }
                    "embed" -> {
                        command.add("-p")
                        command.add("embed")
                    }
                    else -> throw java.lang.IllegalArgumentException("Unknown colorspace: $colorspace")
                }
                when (interpolation) {
                    // "bilinear", "variable-number-gradients", "patterned-pixel-grouping", "adaptive-homogeneity-directed", "none", "none-unscaled", "none-uncropped"
                    "bilinear" -> {
                        command.add("-q")
                        command.add("0")
                    }
                    "VNG" -> {
                        command.add("-q")
                        command.add("1")
                    }
                    "PPG" -> {
                        command.add("-q")
                        command.add("2")
                    }
                    "AHD" -> {
                        command.add("-q")
                        command.add("3")
                    }
                    "none" -> command.add("-d")
                    "none-unscaled" -> command.add("-D")
                    "none-uncropped" -> command.add("-E")
                    else -> throw java.lang.IllegalArgumentException("Unknown interpolation: $interpolation")
                }
                if (medianPasses > 0) {
                    command.add("-m")
                    command.add(medianPasses.toString())
                }
                when (bits) {
                    "16" -> command.add("-6")
                    else -> {}
                }
                command.add("-b")
                command.add(brightness.toString())

                command.add("-O")
                command.add(file.prefixName(outputDirectory, "${name}_").replaceExtension("tif").path)

                command.add(file.path)

                println("Command: $command")

                processBuilder.command(command)
                //processBuilder.directory(file.parentFile)

                val process = processBuilder.start()

                Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
                val exitCode = process.waitFor()
                println("Exit code: $exitCode")
            }

            multi {
                val dcraw: String by arguments
                val aspectRatio: Boolean by arguments
                val rotate: String by arguments
                val whitebalance: String by arguments
                val localX: Optional<Int> by arguments
                val localY: Optional<Int> by arguments
                val localRadius: Optional<Int> by arguments
                val multipliers: Optional<List<Double>> by arguments
                val darkness: Optional<Int> by arguments
                val saturation: Optional<Int> by arguments
                val colorspace: String by arguments
                val interpolation: String by arguments
                val medianPasses: Int by arguments
                val bits: String by arguments
                val brightness: Double by arguments

                for (inputFile in inputFiles) {
                    println("Converting $inputFile")
                    dcraw(dcraw, aspectRatio, rotate, whitebalance, localX, localY, localRadius, multipliers, darkness, saturation, colorspace, interpolation, medianPasses, bits, brightness, inputFile, outputDirectory)
                    println()
                }

                null
            }
        }

    private fun scriptConvertFrames(): Script =
        kimage(0.1) {
            name = "convert-frames"
            title = "Convert a video frames into images"
            description = """
                Convert an a video frames into images.
                """
            arguments {
                string("ffmpeg") {
                    description = """
               The `ffmpeg` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
                    default = "dcraw"
                }
                string("extension") {
                    default = "tif"
                }
            }

            multi {
                val dcraw: String by arguments
                val extension: String by arguments

                val scriptName = this@kimage.name

                for (inputFile in inputFiles) {
                    println("Converting $inputFile")

                    val processBuilder = ProcessBuilder()

                    val command = mutableListOf(dcraw, "-i", inputFile.path)

                    // TODO add script name
                    command.add(inputFile.prefixName(outputDirectory, "${scriptName}_%04d_").replaceExtension(extension).path)

                    println("Command: $command")

                    processBuilder.command(command)

                    val process = processBuilder.start()

                    Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
                    val exitCode = process.waitFor()
                    println("Exit code: $exitCode")
                }

                null
            }
        }

    private fun scriptPickBest(): Script =
        kimage(0.1) {
            name = "pick-best"
            title = "Picks the best images"
            description = """
                Picks the best image and sorts all other images by similarity.
                
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
                int("medianRadius") {
                    description = """
                        The radius of the median filter before measuring for similarity.
                        This is useful to reduce noise.
                        """
                    min = 0
                    default = 1
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
                val medianRadius: Int by arguments
                val radius: Int by arguments
                val prefix: String by arguments

                val measureChannel = Channel.Luminance

                val croppedSize = radius * 2 + 1
                val errorMatrix = DoubleMatrix(croppedSize, croppedSize)
                val croppedMatrices = mutableListOf<Matrix>()
                //val huge = HugeFloatArray(inputFiles.size, croppedSize, croppedSize)

                for (fileIndex in inputFiles.indices) {
                    println("Loading ${inputFiles[fileIndex]}")
                    val inputImage = ImageReader.read(inputFiles[fileIndex])

                    val croppedImage = inputImage.cropCenter(radius, centerX, centerY).medianFilter(medianRadius)
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

    private fun scriptCropBrightest(): Script =
        kimage(0.1) {
            name = "crop-brightest"
            title = "Crop brightest patch"
            description = """
                Crops the brightest part of an image.
                """
            arguments {
                double("percentile") {
                    min = 0.0
                    max = 100.0
                    unit = "% percentile"
                    default = 80.0
                }
                string("channel") {
                    description = """
                        """
                    allowed = listOf("gray", "luminance", "red", "green", "blue")
                    default = "gray"
                }
                int("radius") {
                    description = """
                        The radius around the center of the brightest patch to crop.
                        """
                    min = 1
                }
            }

            single {
                val percentile: Double by arguments
                val channel: String by arguments
                val radius: Int by arguments

                val measureChannel = when (channel) {
                    "gray" -> Channel.Gray
                    "luminance" -> Channel.Luminance
                    "red" -> Channel.Red
                    "green" -> Channel.Green
                    "blue" -> Channel.Blue
                    else -> throw IllegalArgumentException("Unknown channel: $channel")
                }

                val measureMatrix = inputImage[measureChannel]

                val histogram = Histogram()
                histogram.add(measureMatrix)
                val percentileValue = histogram.estimatePercentile(percentile / 100.0)

                var largestPatchWidth = 0
                var largestPatchX = 0
                var largestPatchY = 0

                for (y in 0 until inputImage.height) {
                    var insidePatch = false
                    var patchStartX = 0
                    for (x in 0 until inputImage.width) {
                        if (measureMatrix[x, y] >= percentileValue) {
                            if (!insidePatch) {
                                patchStartX = x
                                insidePatch = true
                            }
                        } else {
                            if (insidePatch) {
                                val patchWidth = x - patchStartX
                                if (patchWidth > largestPatchWidth) {
                                    largestPatchWidth = patchWidth
                                    largestPatchX = patchStartX + largestPatchWidth/2
                                    largestPatchY = y
                                }
                                insidePatch = false
                            }
                        }
                    }
                }

                println("Largest patch:")
                println("  X: $largestPatchX (width $largestPatchWidth)")
                println("  Y: $largestPatchY")
                println()

                if (debugMode) {
                    val debugImage = inputImage.copy()
                    for (x in 0 until largestPatchX-largestPatchWidth/2) {
                        debugImage[Channel.Red][x, largestPatchY] = 1.0
                    }
                    for (x in largestPatchX+largestPatchWidth/2 until debugImage.width) {
                        debugImage[Channel.Red][x, largestPatchY] = 1.0
                    }
                    val debugFile = inputFile.prefixName(outputDirectory, "debug_patch_")
                    println("Saving $debugFile for manual analysis")
                    ImageWriter.write(debugImage, debugFile)
                    println()
                }

                inputImage.cropCenter(radius, largestPatchX, largestPatchY)
            }
        }

    private fun scriptDrizzle(): Script =
        kimage(0.1) {
            name = "drizzle"
            title = "Drizzle multiple images"
            description = """
                Drizzle multiple images.
                """
            arguments {
                double("factor") {
                    default = 2.0
                }
                double("fraction") {
                    default = 0.5
                }
            }

            multi {
                val factor: Double by arguments
                val fraction: Double by arguments

                null
            }
        }

    private fun scriptComposition(): Script =
        kimage(0.1) {
            name = "test-composition"
            title = "Composition of kimage scripts"
            description = """
                KImage scripts calling other kimage scripts.
                """
            arguments {
            }

            multi {
                //val directory = if (inputFiles.isNotEmpty()) inputFiles[0].parentFile else File(".")
                val directory = File(".")

                runScript("filter",
                    inputFiles,
                    "filter" to "median",
                    "radius" to 5)

                runScript("rotate-left",
                    directory.matchingFiles("filter_*"))

            }
        }

    private fun scriptSamplePSF(): Script =
        kimage(0.1) {
            name = "sample-psf"
            title = "Create PSF from image sample"
            description = """
                Create a PSF (Point Spread Function) from an image sample.
                The output image can be used in deconvolution as PSF.
                """
            arguments {
                optionalInt("sampleX") {
                    hint = Hint.ImageX
                }
                optionalInt("sampleY") {
                    hint = Hint.ImageY
                }
                int("medianRadius") {
                    default = 1
                }
                double("smoothRadius") {
                    default = 0.5
                }
                int("radius") {
                    default = 10
                }
            }

            single {
                val sampleX: Optional<Int> by arguments
                val sampleY: Optional<Int> by arguments
                val medianRadius: Int by arguments
                val smoothRadius: Double by arguments
                val radius: Int by arguments

                var m = inputImage[Channel.Gray].cropCenter(radius, sampleX.get(), sampleY.get())

                if (verboseMode) {
                    println("cropped =")
                    println(m.contentToString(true))
                }
                m = m.medianFilter(medianRadius)
                if (verboseMode) {
                    println("median filtered =")
                    println(m.contentToString(true))
                }

                val minValue = m.min()
                if (verboseMode) {
                    println("min = $minValue")
                }
                m -= minValue
                if (verboseMode) {
                    println("subtracted minValue =")
                    println(m.contentToString(true))
                }

                val maxValue = m.max()
                if (verboseMode) {
                    println("max = $maxValue")
                }
                m = m / m.max()
                if (verboseMode) {
                    println("divided maxValue =")
                    println(m.contentToString(true))
                }

                m.onEach { x, y, value ->
                    val dx = (x - radius).toDouble()
                    val dy = (y - radius).toDouble()
                    val r = sqrt(dx*dx + dy*dy) / radius
                    value * (1.0 - smootherstep(smoothRadius, 1.0, r))
                }
                if (verboseMode) {
                    println("smoothstepped =")
                    println(m.contentToString(true))
                }

                MatrixImage(radius*2+1, radius*2+1,
                    Channel.Red to m,
                    Channel.Green to m,
                    Channel.Blue to m)
            }
        }


    private fun scriptDeconvolute(): Script =
        kimage(0.1) {
            name = "deconvolute"
            title = "Deconvolute an image"
            description = """
                Deconvolute an image.
                """
            arguments {
                string("method") {
                    allowed = listOf("lucy", "fft")
                    default = "lucy"
                }
                string("psf") {
                    allowed = listOf("gauss3x3", "gauss5x5", "gauss7x7", "gauss", "moffat", "image")
                    default = "gauss3x3"
                }
                double("background") {
                    enabledWhen = Reference("psf").isEqual("gauss", "moffat")
                    default = 0.0
                }
                double("amplitude") {
                    enabledWhen = Reference("psf").isEqual("gauss", "moffat")
                    default = 1.0
                }
                double("beta") {
                    enabledWhen = Reference("psf").isEqual("moffat")
                    default = 1.0
                }
                double("sigmaX") {
                    enabledWhen = Reference("psf").isEqual("gauss", "moffat")
                    default = 1.0
                }
                double("sigmaY") {
                    enabledWhen = Reference("psf").isEqual("gauss", "moffat")
                    default = 1.0
                }
                double("angle") {
                    enabledWhen = Reference("psf").isEqual("gauss", "moffat")
                    unit = "°"
                    default = 0.0
                }
                optionalFile("psfImage") {
                    enabledWhen = Reference("psf").isEqual("image")
                    isFile = true
                }
                int("radius") {
                    enabledWhen = Reference("psf").isEqual("sample", "gauss", "moffat")
                    unit = "px"
                    default = 3
                }
                int("iterations") {
                    default = 10
                }
            }

            single {
                val method: String by arguments
                val psf: String by arguments
                val background: Double by arguments
                val amplitude: Double by arguments
                val beta: Double by arguments
                val sigmaX: Double by arguments
                val sigmaY: Double by arguments
                val angle: Double by arguments
                val psfImage: Optional<File> by arguments
                val radius: Int by arguments
                val iterations: Int by arguments

                fun rotate(x: Double, y: Double, angle: Double): Pair<Double, Double> {
                    if (angle == 0.0) {
                        return Pair(x, y)
                    }

                    val sinAngle = sin(toRadians(angle))
                    val cosAngle = cos(toRadians(angle))
                    return Pair(
                        +x * cosAngle + y * sinAngle,
                        -x * sinAngle + y * cosAngle)
                }

                fun gauss(
                    x: Double,
                    y: Double,
                    background: Double = 0.0,
                    amplitude: Double = 1.0 - background,
                    sigmaX: Double = 2.0,
                    sigmaY: Double = 2.0,
                    x0: Double = 0.0,
                    y0: Double = 0.0
                ): Double {
                    val dx = x - x0
                    val dy = y - y0
                    return background + amplitude * exp(- ((dx*dx)/2.0/sigmaX/sigmaX + (dy*dy)/2.0/sigmaY/sigmaY))
                }

                fun moffat(
                    x: Double,
                    y: Double,
                    background: Double = 0.0,
                    amplitude: Double = 1.0 - background,
                    beta: Double = 1.0,
                    sigmaX: Double = 2.0,
                    sigmaY: Double = 2.0,
                    x0: Double = 0.0,
                    y0: Double = 0.0
                ): Double {
                    val dx = x - x0
                    val dy = y - y0
                    return background + amplitude / (1.0 + ((dx*dx)/sigmaX/sigmaX + (dy*dy)/sigmaY/sigmaY)).pow(beta)
                }

                fun Matrix.deconvoluteLucyRichardson(psfKernel: Matrix, steps: Int): Matrix {
                    val psfTransposed = psfKernel.transpose()

                    var approx = this
                    for (i in 0 until steps) {
                        val approx2 = approx elementTimes ((this elementDiv (approx.convolute(psfKernel))).convolute(psfTransposed))
                        approx = approx2
                    }
                    return approx
                }

                fun Matrix.deconvoluteFFT(psfKernel: Matrix): Matrix {
                    val paddedMatrix = FFT.padPowerOfTwo(this)
                    val paddedKernel = Matrix.matrixOf(paddedMatrix.width, paddedMatrix.height)
                    paddedKernel.set(psfKernel)
                    val frequencyMatrix = FFT.fft(ComplexMatrix(paddedMatrix))
                    val frequencyKernel = FFT.fft(ComplexMatrix(paddedKernel))
                    val frequencyDeconvoluted = frequencyMatrix elementDiv frequencyKernel
                    frequencyDeconvoluted.onEach { c ->
                        var re = c.re
                        var im = c.im
                        if (!re.isFinite()) {
                            re = 1.0
                        }
                        if (!im.isFinite()) {
                            im = 1.0
                        }
                        Complex(re, im)
                    }
                    val result = FFT.fftInverse(frequencyDeconvoluted)
                    return result.re
                }

                val outputMatrices: MutableMap<Channel, Matrix> = mutableMapOf()
                val psfKernelMatrices: MutableMap<Channel, Matrix> = mutableMapOf()
                for (channel in inputImage.channels) {
                    println("Processing channel: $channel")
                    val psfKernel = when (psf) {
                        "gauss3x3" -> KernelFilter.GaussianBlur3
                        "gauss5x5" -> KernelFilter.GaussianBlur5
                        "gauss7x7" -> KernelFilter.GaussianBlur7
                        "gauss" -> {
                            DoubleMatrix(radius*2+1, radius*2+1) { x, y ->
                                val (x2, y2) = rotate((x - radius).toDouble(), (y - radius).toDouble(), angle)
                                gauss(x2, y2, background, amplitude, sigmaX, sigmaY)
                            }
                        }
                        "moffat" -> {
                            DoubleMatrix(radius*2+1, radius*2+1) { x, y ->
                                val (x2, y2) = rotate((x - radius).toDouble(), (y - radius).toDouble(), angle)
                                moffat(x2, y2, background, amplitude, beta, sigmaX, sigmaY)
                            }
                        }
                        "image" -> {
                            ImageReader.read(psfImage.get())[Channel.Gray]
                        }
                        else -> throw IllegalArgumentException("Unknown psf: $psf")
                    }

                    if (verboseMode) {
                        println("PSF matrix:")
                        println(psfKernel.contentToString(true))
                    }
                    psfKernelMatrices[channel] = psfKernel

                    outputMatrices[channel] = when (method) {
                        "lucy" -> inputImage[channel].deconvoluteLucyRichardson(psfKernel, iterations)
                        "fft" -> inputImage[channel].deconvoluteFFT(psfKernel)
                        else -> throw IllegalArgumentException("Unknown method: $method")
                    }
                }
                println()

                if (debugMode) {
                    val m = psfKernelMatrices.iterator().next().value
                    val psfKernelImage = MatrixImage(m.width, m.height, psfKernelMatrices.keys.toList()) { channel, _, _ -> psfKernelMatrices[channel]!! }
                    val psfFile = inputFile.prefixName(outputDirectory, "psf_")
                    println("Saving $psfFile for manual analysis")
                    ImageWriter.write(psfKernelImage, psfFile)
                }

                MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
            }
        }

    private fun scriptWhitebalance(): Script =
        kimage(0.1) {
            name = "whitebalance"
            title = "Correct the whitebalance of an image"
            description = """
                Correct the whitebalance of an image.
                """
            arguments {
                string ("whitebalance") {
                    description = """
                The whitebalancing algorithm.
                
                - `custom` specifies the concrete multipliers for `red`, `green` and `blue` channel.
                - `global` uses the median of the entire input image to determine the gray value.
                - `highlight` uses the median of the highlighted pixels of the entire input image to determine the gray value.
                   Use `highlight` to specify the percentile of the pixels that should be used
                - `local` uses the median of a region centered at `localX`/`localY` with a radius of `localRadius` pixels.
                """
                    allowed = listOf("custom", "global", "highlight", "local")
                    default = "highlight"
                }
                optionalInt("localX") {
                    description = """
                The center on the x axis of the local area to determine the gray value for white balancing.
                """
                    hint = Hint.ImageX
                    enabledWhen = Reference("whitebalance").isEqual("local")
                }
                optionalInt("localY") {
                    description = """
                The center on the y axis of the local area to determine the gray value for white balancing.
                """
                    hint = Hint.ImageY
                    enabledWhen = Reference("whitebalance").isEqual("local")
                }
                int("localRadius") {
                    description = """
                The radius of the local area to determine the gray value for white balancing.
                """
                    enabledWhen = Reference("whitebalance").isEqual("local")
                    default = 10
                }
                double("highlight") {
                    description = """
                The percentile of the hightlights to determine the gray value for white balancing.
                """
                    unit = "% percentile"
                    enabledWhen = Reference("whitebalance").isEqual("highlight")
                    default = 80.0
                }
                string("highlightChannel") {
                    description = """
                The channel to measure the highlights to determine the gray value for white balancing.
                """
                    enabledWhen = Reference("whitebalance").isEqual("highlight")
                    allowed = listOf("red", "green", "blue", "gray", "luminance")
                    default = "gray"
                }
                boolean("ignoreOverExposed") {
                    description = """
                        Ignore pixels where at least one color channel is at maximum level.
                        This will ignore overexposed pixels.
                        """
                    enabledWhen = Reference("whitebalance").isEqual("highlight")
                    default = true
                }
                optionalDouble("red") {
                    description = """
                The red value for custom white balancing.
                """
                    enabledWhen = Reference("whitebalance").isEqual("custom")
                }
                optionalDouble("green") {
                    description = """
                The green value for custom white balancing.
                """
                    enabledWhen = Reference("whitebalance").isEqual("custom")
                }
                optionalDouble("blue") {
                    description = """
                The blue  value for custom white balancing.
                """
                    enabledWhen = Reference("whitebalance").isEqual("custom")
                }
            }

            single {
                var whitebalance: String by arguments
                var localX: Optional<Int> by arguments
                var localY: Optional<Int> by arguments
                val localRadius: Int by arguments
                val highlight: Double by arguments
                val highlightChannel: String by arguments
                val ignoreOverExposed: Boolean by arguments
                var red: Optional<Double> by arguments
                var green: Optional<Double> by arguments
                var blue: Optional<Double> by arguments

                if (!localX.isPresent) {
                    localX = Optional.of(inputImage.width / 2)
                }
                if (!localY.isPresent) {
                    localY = Optional.of(inputImage.height/ 2)
                }

                val redMatrix = inputImage[Channel.Red]
                val greenMatrix = inputImage[Channel.Green]
                val blueMatrix = inputImage[Channel.Blue]

                when (whitebalance) {
                    "custom" -> {
                        if (red.isPresent) {
                            red = Optional.of(1.0 / red.get())
                        }
                        if (green.isPresent) {
                            green = Optional.of(1.0 / green.get())
                        }
                        if (blue.isPresent) {
                            blue = Optional.of(1.0 / blue.get())
                        }
                    }
                    "global" -> {
                        red = Optional.of(redMatrix.median())
                        green = Optional.of(greenMatrix.median())
                        blue = Optional.of(blueMatrix.median())
                    }
                    "highlight" -> {
                        val channel = when (highlightChannel) {
                            "red" -> Channel.Red
                            "green" -> Channel.Green
                            "blue" -> Channel.Blue
                            "gray" -> Channel.Gray
                            "luminance" -> Channel.Luminance
                            else -> throw IllegalArgumentException("Unknown channel: $highlightChannel")
                        }
                        val highlightMatrix = inputImage[channel]
                        val histogram = Histogram()
                        histogram.add(highlightMatrix)
                        val highlightValue = histogram.estimatePercentile(highlight / 100.0)

                        var overExposedCount = 0
                        val redValues = mutableListOf<Double>()
                        val greenValues = mutableListOf<Double>()
                        val blueValues = mutableListOf<Double>()
                        for (y in 0 until highlightMatrix.height) {
                            for (x in 0 until highlightMatrix.width) {
                                if (highlightMatrix[x, y] >= highlightValue) {
                                    val r = redMatrix[x, y]
                                    val g = greenMatrix[x, y]
                                    val b = blueMatrix[x, y]
                                    if (ignoreOverExposed && (r >= 1.0 || g >= 1.0 || b >= 1.0)) {
                                        overExposedCount++
                                    } else {
                                        redValues += r
                                        greenValues += g
                                        blueValues += b
                                    }
                                }
                            }
                        }
                        if (verboseMode && ignoreOverExposed) {
                            println("Over exposure: $overExposedCount pixels ignored")
                        }
                        if (verboseMode) {
                            println("Highlight ${highlight} (>= $highlightValue in $channel): ${redValues.size} pixels found")
                        }
                        red = Optional.of(redValues.median())
                        green = Optional.of(greenValues.median())
                        blue = Optional.of(blueValues.median())
                    }
                    "local" -> {
                        val centerX = localX.get()
                        val centerY = localY.get()
                        red = Optional.of(redMatrix.cropCenter(localRadius, centerX, centerY, false).median())
                        green = Optional.of(greenMatrix.cropCenter(localRadius, centerX, centerY, false).median())
                        blue = Optional.of(blueMatrix.cropCenter(localRadius, centerX, centerY, false).median())
                    }
                    else -> throw IllegalArgumentException("Unknown whitebalance: $whitebalance")
                }

                if (!red.isPresent()) {
                    red = Optional.of(1.0)
                }
                if (!green.isPresent()) {
                    green = Optional.of(1.0)
                }
                if (!blue.isPresent()) {
                    blue = Optional.of(1.0)
                }

                val maxFactor = max(red.get(), max(green.get(), blue.get()))
                var redFactor = if (red.get() == 0.0) 1.0 else maxFactor / red.get()
                var greenFactor = if (green.get() == 0.0) 1.0 else maxFactor / green.get()
                var blueFactor = if (blue.get() == 0.0) 1.0 else maxFactor / blue.get()

                println("Whitebalance Factor:")
                println("  red =   $redFactor")
                println("  green = $greenFactor")
                println("  blue =  $blueFactor")
                println()

                var redOffset = 0.0
                var greenOffset = 0.0
                var blueOffset = 0.0

                redMatrix.onEach { v -> (v - redOffset) * redFactor  }
                greenMatrix.onEach { v -> (v - greenOffset) * greenFactor  }
                blueMatrix.onEach { v -> (v - blueOffset) * blueFactor  }

                MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to redMatrix,
                    Channel.Green to greenMatrix,
                    Channel.Blue to blueMatrix)
            }
        }

    private fun scriptDebayer(): Script =
        kimage(0.1) {
            name = "debayer"
            title = "Debayer a raw image into a color image"
            description = """
                Debayer the mosaic of a raw image into a color image.
                """
            arguments {
                optionalFile("badpixels") {
                    isFile = true
                }
                string("pattern") {
                    allowed = listOf("rggb", "bggr", "gbrg", "grbg")
                    default = "rggb"
                }
                optionalDouble("red") {
                }
                optionalDouble("green") {
                }
                optionalDouble("blue") {
                }
                string("interpolation") {
                    allowed = listOf("superpixel", "none", "nearest", "bilinear")
                    default = "bilinear"
                }
            }

            single {
                val badpixels: Optional<File> by arguments
                val pattern: String by arguments
                var red: Optional<Double> by arguments
                var green: Optional<Double> by arguments
                var blue: Optional<Double> by arguments
                val interpolation: String by arguments

                val badpixelCoords = if (badpixels.isPresent()) {
                    badpixels.get().readLines()
                        .filter { !it.isBlank() }
                        .filter { !it.startsWith("#") }
                        .map {
                            val values = it.trim().split(Regex("\\s+"))
                            if (values.size >= 2) {
                                Pair(Integer.parseInt(values[0]), Integer.parseInt(values[1]))
                            } else {
                                throw java.lang.IllegalArgumentException("Format must be 'x y'")
                            }
                        }.toSet()
                } else {
                    setOf()
                }

                val (width, height) = when (interpolation) {
                    "superpixel" -> Pair(inputImage.width / 2, inputImage.height / 2)
                    else -> Pair(inputImage.width, inputImage.height)
                }

                val (rX, rY) = when (pattern) {
                    "rggb" -> Pair(0, 0)
                    "bggr" -> Pair(1, 1)
                    "gbrg" -> Pair(0, 1)
                    "grbg" -> Pair(0, 1)
                    else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
                }
                val (g1X, g1Y) = when (pattern) {
                    "rggb" -> Pair(0, 1)
                    "bggr" -> Pair(0, 1)
                    "gbrg" -> Pair(0, 0)
                    "grbg" -> Pair(0, 0)
                    else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
                }
                val (g2X, g2Y) = when (pattern) {
                    "rggb" -> Pair(1, 0)
                    "bggr" -> Pair(1, 0)
                    "gbrg" -> Pair(1, 1)
                    "grbg" -> Pair(1, 1)
                    else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
                }
                val (bX, bY) = when (pattern) {
                    "rggb" -> Pair(1, 1)
                    "bggr" -> Pair(0, 0)
                    "gbrg" -> Pair(0, 1)
                    "grbg" -> Pair(1, 0)
                    else -> throw java.lang.IllegalArgumentException("Unknown pattern: $pattern")
                }

                val mosaic = inputImage[Channel.Gray]

                println("Bad pixels: $badpixelCoords")
                for (badpixelCoord in badpixelCoords) {
                    val x = badpixelCoord.first
                    val y = badpixelCoord.second

                    val surroundingValues = mutableListOf<Double>()
                    for (dy in -2..+2 step 4) {
                        for (dx in -2..+2 step 4) {
                            if (mosaic.isInside(x + dx, y + dy) && !badpixelCoords.contains(
                                    Pair(
                                        x + dx,
                                        y + dy
                                    )
                                )
                            ) {
                                surroundingValues.add(mosaic[x + dx, y + dy])
                            }
                        }
                    }

                    mosaic[x, y] = surroundingValues.median()
                }

                val mosaicRedMatrix = DoubleMatrix(mosaic.width / 2, mosaic.height / 2)
                val mosaicGreen1Matrix = DoubleMatrix(mosaic.width / 2, mosaic.height / 2)
                val mosaicGreen2Matrix = DoubleMatrix(mosaic.width / 2, mosaic.height / 2)
                val mosaicBlueMatrix = DoubleMatrix(mosaic.width / 2, mosaic.height / 2)
                val mosaicGrayMatrix = DoubleMatrix(mosaic.width / 2, mosaic.height / 2)

                for (y in 0 until inputImage.height step 2) {
                    for (x in 0 until inputImage.width step 2) {
                        val r = mosaic[x+rX, y+rY]
                        val g1 = mosaic[x+g1X, y+g1Y]
                        val g2 = mosaic[x+g2X, y+g2Y]
                        val b = mosaic[x+bX, y+bY]
                        val gray = (r + r + g1 + g2 + b + b) / 6

                        mosaicRedMatrix[x/2, y/2] = r
                        mosaicGreen1Matrix[x/2, y/2] = g1
                        mosaicGreen2Matrix[x/2, y/2] = g2
                        mosaicBlueMatrix[x/2, y/2] = b
                        mosaicGrayMatrix[x/2, y/2] = gray
                    }
                }

                if (red.isPresent) {
                    red = Optional.of(1.0 / red.get())
                }
                if (green.isPresent) {
                    green = Optional.of(1.0 / green.get())
                }
                if (blue.isPresent) {
                    blue = Optional.of(1.0 / blue.get())
                }

                if (!red.isPresent()) {
                    red = Optional.of(1.0)
                }
                if (!green.isPresent()) {
                    green = Optional.of(1.0)
                }
                if (!blue.isPresent()) {
                    blue = Optional.of(1.0)
                }

                println("  red =   $red")
                println("  green = $green")
                println("  blue =  $blue")
                println()

                val maxFactor = max(red.get(), max(green.get(), blue.get()))
                var redFactor = maxFactor / red.get()
                var greenFactor = maxFactor / green.get()
                var blueFactor = maxFactor / blue.get()

                println("Whitebalance:")
                println("  red =   $redFactor")
                println("  green = $greenFactor")
                println("  blue =  $blueFactor")

                var redOffset = 0.0
                var greenOffset = 0.0
                var blueOffset = 0.0

                mosaicRedMatrix.onEach { v -> (v - redOffset) * redFactor  }
                mosaicGreen1Matrix.onEach { v -> (v - greenOffset) * greenFactor  }
                mosaicGreen2Matrix.onEach { v -> (v - greenOffset) * greenFactor  }
                mosaicBlueMatrix.onEach { v -> (v - blueOffset) * blueFactor  }

                val redMatrix = Matrix.matrixOf(width, height)
                val greenMatrix = Matrix.matrixOf(width, height)
                val blueMatrix = Matrix.matrixOf(width, height)

                when (interpolation) {
                    "superpixel" -> {
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val r = mosaicRedMatrix[x, y]
                                val g1 = mosaicGreen1Matrix[x, y]
                                val g2 = mosaicGreen2Matrix[x, y]
                                val b = mosaicBlueMatrix[x, y]

                                redMatrix[x, y] = r
                                greenMatrix[x, y] = (g1+g2)/2
                                blueMatrix[x, y] = b
                            }
                        }
                    }
                    "none" -> {
                        for (y in 0 until height step 2) {
                            for (x in 0 until width step 2) {
                                val r = mosaicRedMatrix[x/2, y/2]
                                val g1 = mosaicGreen1Matrix[x/2, y/2]
                                val g2 = mosaicGreen2Matrix[x/2, y/2]
                                val b = mosaicBlueMatrix[x/2, y/2]

                                redMatrix[x+rX, y+rY] = r
                                greenMatrix[x+g1X, y+g1Y] = g1
                                greenMatrix[x+g2X, y+g2Y] = g2
                                blueMatrix[x+bX, y+bY] = b
                            }
                        }
                    }
                    "nearest" -> {
                        for (y in 0 until height step 2) {
                            for (x in 0 until width step 2) {
                                val r = mosaicRedMatrix[x / 2, y / 2]
                                val g1 = mosaicGreen1Matrix[x / 2, y / 2]
                                val g2 = mosaicGreen2Matrix[x / 2, y / 2]
                                val b = mosaicBlueMatrix[x / 2, y / 2]

                                redMatrix[x + 0, y + 0] = r
                                redMatrix[x + 1, y + 0] = r
                                redMatrix[x + 0, y + 1] = r
                                redMatrix[x + 1, y + 1] = r
                                blueMatrix[x + 0, y + 0] = b
                                blueMatrix[x + 1, y + 0] = b
                                blueMatrix[x + 0, y + 1] = b
                                blueMatrix[x + 1, y + 1] = b
                                greenMatrix[x + 0, y + 0] = g1
                                greenMatrix[x + 1, y + 0] = g1
                                greenMatrix[x + 0, y + 1] = g2
                                greenMatrix[x + 1, y + 1] = g2
                            }
                        }
                    }
                    "bilinear" -> {
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val dx = x % 2
                                val dy = y % 2

                                val r: Double
                                val g: Double
                                val b: Double
                                if (dx == rX && dy == rY) {
                                    r = mosaic[x, y]
                                    g = (mosaic[x - 1, y] + mosaic[x + 1, y] + mosaic[x,
                                            y - 1
                                    ] + mosaic[x, y + 1]) / 4
                                    b = (mosaic[x - 1, y - 1] + mosaic[x - 1,
                                            y + 1
                                    ] + mosaic[x + 1, y - 1] + mosaic[x + 1, y + 1]) / 4
                                } else if (dx == bX && dy == bY) {
                                    r = (mosaic[x - 1, y - 1] + mosaic[x - 1, y + 1] + mosaic[x + 1,
                                            y - 1
                                    ] + mosaic[x + 1, y + 1]) / 4
                                    g = (mosaic[x - 1, y] + mosaic[x + 1, y] + mosaic[x,
                                            y - 1
                                    ] + mosaic[x, y + 1]) / 4
                                    b = mosaic[x, y]
                                } else {
                                    g = mosaic[x, y]
                                    if ((x - 1) % 2 == rX) {
                                        r = (mosaic[x - 1, y] + mosaic[x + 1, y]) / 2
                                        b = (mosaic[x, y - 1] + mosaic[x, y + 1]) / 2
                                    } else {
                                        r = (mosaic[x, y - 1] + mosaic[x, y + 1]) / 2
                                        b = (mosaic[x - 1, y] + mosaic[x + 1, y]) / 2
                                    }
                                }

                                redMatrix[x, y] = (r - redOffset) * redFactor
                                greenMatrix[x, y] = (g - greenOffset) * greenFactor
                                blueMatrix[x, y] = (b - blueOffset) * blueFactor
                            }
                        }
                    }
                    else -> throw java.lang.IllegalArgumentException("Unknown interpolation: $interpolation")
                }

                MatrixImage(width, height,
                    Channel.Red to redMatrix,
                    Channel.Green to greenMatrix,
                    Channel.Blue to blueMatrix)
            }
        }

    private fun scriptRemoveOutliers(): Script =
        kimage(0.1) {
            name = "remove-outliers"
            title = "Remove outliers"
            description = """
                Rotate image 90 degrees left.
                """
            arguments {
                double("kappa") {
                    description = """
                        The kappa factor is used in sigma-clipping of sample values to determine the outlier values.
                        """
                    default = 10.0
                }
                optionalDouble("low") {
                    description = """
                        The low threshold to remove outliers below.
                        The default `low` value is calculated from the image using the `kappa` factor.
                        """
                }
                optionalDouble("high") {
                    description = """
                        The high threshold to remove outliers below.
                        The default `high` value is calculated from the image using the `kappa` factor.
                        """
                }
                string("replace") {
                    description = """
                        The method to replace the outlier values.
                        - `global-median` replaces outlier values with the global median of the current channel.
                          All outliers will be replaced with the same value.
                        - `local-median` replaces outlier values with the local median of the current channel
                          using the `local-radius`.
                        """
                    allowed = listOf("global-median", "local-median")
                    default = "global-median"
                }
                int("localRadius") {
                    description = """
                        The radius used in the replace method `local-median` to replace an outlier value. 
                        """
                    enabledWhen = Reference("replace").isEqual("local-median")
                    min = 1
                    default = 10
                }
            }

            single {
                val kappa: Double by arguments
                var low: Optional<Double> by arguments
                var high: Optional<Double> by arguments
                val replace: String by arguments
                val localRadius: Int by arguments

                val badpixels: MutableSet<Pair<Int, Int>> = mutableSetOf()
                val badpixelMatrices = mutableMapOf<Channel, Matrix>()
                val outputMatrices = mutableMapOf<Channel, Matrix>()
                for (channel in inputImage.channels) {
                    println("Processing channel: $channel")

                    val matrix = inputImage[channel]

                    val globalMedian = matrix.fastMedian()
                    val globalStddev = matrix.stddev()
                    if (!low.isPresent) {
                        low = Optional.of(globalMedian - globalStddev * kappa)
                    }
                    if (!high.isPresent) {
                        high = Optional.of(globalMedian + globalStddev * kappa)
                    }

                    if (verboseMode) {
                        println("Median value: $globalMedian")
                        println("Standard deviation: $globalStddev")
                        println("Clipping range: $low .. $high")
                    }

                    var outlierCount = 0
                    val outputMatrix = matrix.create()
                    val badpixelMatrix = matrix.create()
                    for (y in 0 until matrix.height) {
                        for (x in 0 until matrix.width) {
                            val value = matrix[x, y]
                            outputMatrix[x, y] = if (value in low.get()..high.get()) {
                                matrix[x, y]
                            } else {
                                badpixels.add(Pair(x, y))
                                outlierCount++
                                val replacedValue = when (replace) {
                                    "global-median" -> globalMedian
                                    "local-median" -> matrix.medianAround(x, y, localRadius)
                                    else -> throw java.lang.IllegalArgumentException("Unknown replace method: $replace")
                                }
                                badpixelMatrix[x, y] = replacedValue
                                replacedValue
                            }
                        }
                    }
                    println("Found $outlierCount outliers")
                    println()

                    badpixelMatrices[channel] = badpixelMatrix
                    outputMatrices[channel] = outputMatrix
                }

                val file = inputFile.prefixName(outputDirectory, "badpixels_").suffixExtension(".txt")
                println("Saving $file")
                val badpixelWriter = PrintWriter(FileWriter(file))

                for (badpixel in badpixels) {
                    badpixelWriter.println(String.format("%6d %6d 0", badpixel.first, badpixel.second))
                    if (debugMode) {
                        val badPixelFile = inputFile.prefixName(outputDirectory, "badpixel_${badpixel.first}_${badpixel.second}_")
                        val badPixelCrop = inputImage.cropCenter(5, badpixel.first, badpixel.second).scaleBy(
                            4.0,
                            4.0,
                            0.0,
                            0.0,
                            Scaling.Nearest
                        )
                        ImageWriter.write(badPixelCrop, badPixelFile)
                    }
                }
                badpixelWriter.close()

                if (debugMode) {
                    val badpixelImageFile = inputFile.prefixName(outputDirectory, "badpixel_")
                    println("Saving $badpixelImageFile")
                    val badpixelImage = MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> badpixelMatrices[channel]!! }
                    ImageWriter.write(badpixelImage, badpixelImageFile)
                }

                MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
            }
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
                    val medianFile = inputFile.prefixName(outputDirectory, "median_")
                    println("Writing $medianFile")
                    ImageWriter.write(medianImage, medianFile)
                }

                if (verboseMode) println("Running gaussian blur filter ...")
                val backgroundImage = medianImage.gaussianBlurFilter(blurFilterSize)
                if (debugMode) {
                    val backgroundFile = inputFile.prefixName(outputDirectory, "background_")
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
                    hint = Hint.ImageX
                    min = 0
                }
                optionalInt("centerY") {
                    description = """
                        The Y coordinate of the center to check for alignment.
                        The default value is calculated from the base image.
                        """
                    hint = Hint.ImageY
                    min = 0
                }
                int("medianRadius") {
                    min = 0
                    default = 1
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
                double("subPixelStep") {
                    description = """
                        """
                    //allowed = listOf(0.5, 0.2, 0.1, 0.05, 0.02, 0.01)
                    default = 0.1
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
                    enabledWhen = Reference("saveBad").isEqual(true)
                    default = "badaligned"
                }
                boolean("sort") {
                    description = "Sort output files by error (best aligned first)."
                    default = true
                }
            }

            multi {
                val medianRadius: Int by arguments

                val baseInputFile = inputFiles[0]
                println("Loading base image: $baseInputFile")
                var baseImage = ImageReader.read(baseInputFile)
                println("Base image: $baseImage")
                println()

                if (medianRadius > 0) {
                    baseImage = baseImage.medianFilter(medianRadius)
                }

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
                val subPixelStep: Double by arguments
                val prefix: String by arguments
                val saveBad: Boolean by arguments
                val prefixBad: String by arguments
                val sort: Boolean by arguments

                println("Arguments (calculated from input):")
                println("  checkRadius = ${checkRadius.get()}")
                println("  searchRadius = ${searchRadius.get()}")
                println("  centerX = ${centerX.get()}")
                println("  centerY = ${centerY.get()}")
                println()

                if (debugMode) {
                    val checkImage = baseImage.cropCenter(checkRadius.get(), centerX.get(), centerY.get())
                    val checkFile = baseInputFile.prefixName(outputDirectory, "check_")
                    println("Saving $checkFile for manual analysis")
                    ImageWriter.write(checkImage, checkFile)
                    println()
                }

                val outputFilesAlignment = mutableListOf<Pair<File, Alignment>>()

                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")

                    val image = ImageReader.read(inputFile)
                    if (verboseMode) println("Aligning image: $image")
                    val medianImage = if (medianRadius == 0) image else image.medianFilter(medianRadius)

                    val alignment = imageAligner.align(
                        baseImage,
                        medianImage,
                        centerX = centerX.get(),
                        centerY = centerY.get(),
                        maxOffset = searchRadius.get(),
                        subPixelStep = subPixelStep,
                        createErrorMatrix = debugMode
                    )
                    println("Alignment: $alignment")

                    alignment.errorMatrix0?.let {
                        val normalizedErrorMatrix = it / it.max()
                        val errorFile = inputFile.prefixName(outputDirectory, "error0_${prefix}_")
                        println("Saving $errorFile for manual analysis")
                        val errorImage = MatrixImage(normalizedErrorMatrix.width, normalizedErrorMatrix.height,
                            Channel.Red to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 1.0 else v },
                            Channel.Green to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 0.0 else v },
                            Channel.Blue to normalizedErrorMatrix).copy().onEach { v -> if (v < 0) 0.0 else v }
                        ImageWriter.write(errorImage, errorFile)
                    }
                    alignment.errorMatrix1?.let {
                        val normalizedErrorMatrix = it / it.max()
                        val errorFile = inputFile.prefixName(outputDirectory, "error1_${prefix}_")
                        println("Saving $errorFile for manual analysis")
                        val errorImage = MatrixImage(normalizedErrorMatrix.width, normalizedErrorMatrix.height,
                            Channel.Red to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 1.0 else v },
                            Channel.Green to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 0.0 else v },
                            Channel.Blue to normalizedErrorMatrix).copy().onEach { v -> if (v < 0) 0.0 else v }
                        ImageWriter.write(errorImage, errorFile)
                    }
                    alignment.errorMatrix2?.let {
                        val normalizedErrorMatrix = it / it.max()
                        val errorFile = inputFile.prefixName(outputDirectory, "error2_${prefix}_")
                        println("Saving $errorFile for manual analysis")
                        val errorImage = MatrixImage(normalizedErrorMatrix.width, normalizedErrorMatrix.height,
                            Channel.Red to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 1.0 else v },
                            Channel.Green to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 0.0 else v },
                            Channel.Blue to normalizedErrorMatrix).copy().onEach { v -> if (v < 0) 0.0 else v }
                        ImageWriter.write(errorImage, errorFile)
                    }
                    alignment.errorMatrix3?.let {
                        val normalizedErrorMatrix = it / it.max()
                        val errorFile = inputFile.prefixName(outputDirectory, "error3_${prefix}_")
                        println("Saving $errorFile for manual analysis")
                        val errorImage = MatrixImage(normalizedErrorMatrix.width, normalizedErrorMatrix.height,
                            Channel.Red to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 1.0 else v },
                            Channel.Green to normalizedErrorMatrix.copy().onEach { v -> if (v < 0) 0.0 else v },
                            Channel.Blue to normalizedErrorMatrix).copy().onEach { v -> if (v < 0) 0.0 else v }
                        ImageWriter.write(errorImage, errorFile)
                    }

                    val alignedImage = if (alignment.subPixelX != 0.0 || alignment.subPixelY != 0.0) {
                        image.scaleBy(1.0, 1.0, alignment.subPixelX, alignment.subPixelY).crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
                    } else {
                        image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
                    }

                    //val error = baseImage.averageError(alignedImage)
                    val error = alignment.error
                    if (error <= errorThreshold) {
                        val alignedFile = inputFile.prefixName(outputDirectory, "${prefix}_")
                        println("Good : Error $error <= $errorThreshold : saving $alignedFile")
                        ImageWriter.write(alignedImage, alignedFile)
                        outputFilesAlignment.add(Pair(alignedFile, alignment))
                    } else {
                        if (saveBad) {
                            val badalignedFile = inputFile.prefixName(outputDirectory, "${prefixBad}_")
                            println("Bad  : Error $error > $errorThreshold : saving $badalignedFile")
                            ImageWriter.write(alignedImage, badalignedFile)
                            outputFilesAlignment.add(Pair(badalignedFile, alignment))
                        } else {
                            println("Error $error > $errorThreshold : ignoring badly aligned image")
                        }
                    }

                    if (debugMode) {
                        val deltaFile = inputFile.prefixName(outputDirectory, "delta_${prefix}_")
                        println("Saving $deltaFile for manual analysis")
                        val deltaImage = deltaChannel(baseImage, alignedImage)
                        ImageWriter.write(deltaImage, deltaFile)
                    }

                    println()
                }

                // sort images by error
                if (sort) {
                    outputFilesAlignment.sortBy { it.second.error }

                    var outputFileIndex = 0
                    for (outputFileAlignment in outputFilesAlignment) {
                        val fileName = outputFileAlignment.first.name
                        val sortString = String.format("_%04d", outputFileIndex)
                        val sortedFileName = when {
                            fileName.startsWith(prefix) -> prefix + sortString + fileName.removePrefix(prefix)
                            fileName.startsWith(prefixBad) -> prefixBad + sortString + fileName.removePrefix(prefixBad)
                            else -> sortString + fileName
                        }
                        println("Renaming $fileName to $sortedFileName")
                        Files.move(outputFileAlignment.first.toPath(), File(outputFileAlignment.first.parent, sortedFileName).toPath(), StandardCopyOption.REPLACE_EXISTING)
                        outputFileIndex++;
                    }

                    println()
                }
            }
        }

    private fun scriptInfo(): Script =
        kimage(0.1) {
            name = "info"
            title = "Print info about images"
            arguments {
            }

            multi {
                println(String.format("%-40s %6s %6s %12s %5s %5s %-8s %-8s", "Name", "Exists", "Type", "Bytes", "Width", "Height", "Median", "Stddev"))
                for (file in inputFiles) {
                    val fileSize = if (file.exists()) Files.size(file.toPath()) else 0
                    val fileType = if (file.isFile()) "File" else if (file.isDirectory) "Dir" else "Other"

                    print(String.format("%-40s %6s %6s %12d", file.name, file.exists(), fileType, fileSize))
                    if (file.isFile()) {
                        try {
                            val image = ImageReader.read(file)

                            print(
                                String.format(
                                    "%5d %5d %8.5f %8.5f",
                                    image.width,
                                    image.height,
                                    image.values().median(),
                                    image.values().stddev()
                                )
                            )
                        } catch (ex: Exception) {
                            // ignore
                        }
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

                val outputFile = inputFiles[0].prefixName(outputDirectory, "hdr_")
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
                        light = light / flat.get() * flat.get().values().max()
                    }

                    val outputFile = inputFile.prefixName(outputDirectory, "calibrated_")
                    println("Saving $outputFile")
                    ImageWriter.write(light, outputFile)
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
                int("radius") {
                    default = 100
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

                fun sigmaClipPointGrid(image: Image, grid: List<Pair<Int, Int>>, radius: Int, kappa: Double = 0.5): List<Pair<Int, Int>> {
                    val gridWithMedian = grid.map {
                        val median = image.cropCenter(radius, it.first, it.second).values().fastMedian()
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
                val radius: Int by arguments
                val kappa: Double by arguments

                val grid = pointGrid(inputImage.width, inputImage.height, gridSize, gridSize)
                val clippedGrid = sigmaClipPointGrid(inputImage, grid, radius, kappa)
                if (debugMode) {
                    val gridFile = inputFile.prefixName(outputDirectory, "grid_")
                    val gridImage = inputImage.drawAwt { gc ->
                        gc.paint = java.awt.Color.RED
                        for (pair in grid) {
                            gc.drawRect(pair.first-radius/2, pair.second-radius/2, radius, radius)
                        }
                        gc.paint = java.awt.Color.GREEN
                        for (pair in clippedGrid) {
                            gc.drawRect(pair.first-radius/2, pair.second-radius/2, radius, radius)
                        }
                    }
                    println("Writing $gridFile")
                    ImageWriter.write(gridImage, gridFile)
                }
                if (verboseMode) {
                    println("The ${grid.size} points of the grid have been reduced to ${clippedGrid.size} by sigma-clipping")
                }

                var backgroundImage = inputImage.interpolate(clippedGrid)
                if (debugMode) {
                    val backgroundFile = inputFile.prefixName(outputDirectory, "background_")
                    println("Writing $backgroundFile")
                    ImageWriter.write(backgroundImage, backgroundFile)
                }

                if (verboseMode) println("Subtracting $removePercent% background glow from original image ...")
                backgroundImage = backgroundImage * (removePercent/100.0)

                if (debugMode) {
                    val deltaFile = inputFile.prefixName(outputDirectory, "delta_")
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
            title = "Remove the vignette effect from image"
            description = """
                Calculates a statistical model of the vignette effect of the input image and removes it.
                """
            arguments {
                optionalInt("centerX") {
                    description = """
                        The X coordinate of the center of the vignette effect.
                        The default value is the center of the image.
                        """
                    hint = Hint.ImageX
                }
                optionalInt("centerY") {
                    description = """
                        The Y coordinate of the center of the vignette effect.
                        The default value is the center of the image.
                        """
                    hint = Hint.ImageY
                }
                string("channel") {
                    description = """
                        Controls which channels are used to calculate the vignette effect.
                        The `rgb` channel calculates the effect on the three color channels separately.
                        """
                    allowed = listOf("rgb", "gray", "luminance", "red", "green", "blue")
                    default = "rgb"
                }
                string("model") {
                    description = """
                        The mathematical model use to calculate the vignette effect.
                        """
                    allowed = listOf("gauss", "polynomial", "auto")
                    default = "auto"
                }
                double("kappa") {
                    description = """
                        The kappa factor is used in sigma-clipping of sample values to determine the vignette effect.
                        """
                    default = 100.0
                }
            }
            single {
                fun polynomialFunction(x: Double, coefficients: DoubleArray): Double {
                    var xPower = 1.0
                    var sum = 0.0
                    for (coefficient in coefficients) {
                        sum += coefficient * xPower
                        xPower = xPower * x
                    }
                    return sum
                }
                fun gaussFunction(x: Double, amplitude: Double = 1.0, mean: Double = 0.0, sigma: Double = 1.0): Double {
                    val dx = x - mean
                    return amplitude * exp(dx*dx/-2.0/(sigma*sigma))
                }


                val kappa: Double by arguments
                var centerX: Optional<Int> by arguments
                var centerY: Optional<Int> by arguments
                val channel: String by arguments
                val model: String by arguments

                val channels = when (channel) {
                    "gray" -> listOf(Channel.Gray)
                    "luminance" -> listOf(Channel.Luminance)
                    "red" -> listOf(Channel.Red)
                    "green" -> listOf(Channel.Green)
                    "blue" -> listOf(Channel.Blue)
                    "rgb" -> listOf(Channel.Red, Channel.Green, Channel.Blue)
                    else -> throw IllegalArgumentException("Unknown channel: $channel")
                }
                val channelMatrices = mutableListOf<Matrix>()

                for (channelIndex in channels.indices) {
                    val channel = channels[channelIndex]

                    println("Processing $channel channel")
                    println()

                    val matrix = inputImage[channel]

                    val totalMedian = matrix.fastMedian()
                    val totalStddev = matrix.stddev()
                    val low = totalMedian - totalStddev * kappa
                    val high = totalMedian + totalStddev * kappa

                    if (verboseMode) {
                        println("Median value: $totalMedian")
                        println("Standard deviation: $totalStddev")
                        println("Sigma clipping range: $low .. $high")
                    }

                    if (!centerX.isPresent) {
                        val csvWriter = if (verboseMode) {
                            val file = inputFile.prefixName(outputDirectory, "find_center_x_${channel}_").suffixExtension(".csv")
                            println("Saving $file")
                            val csvWriter = PrintWriter(FileWriter(file))
                            csvWriter.println("  Y, Amplitude, Mean, Sigma")
                            csvWriter
                        } else {
                            null
                        }
                        val centerMeans = DoubleArray(inputImage.height)
                        for (y in 0 until inputImage.height) {
                            val points = WeightedObservedPoints()
                            for (x in 0 until inputImage.width) {
                                val value = matrix[x, y]
                                if (value in low..high) {
                                    points.add(x.toDouble(), value)
                                }
                            }
                            val gaussFit = GaussianCurveFitter.create().fit(points.toList())
                            csvWriter?.let {
                                it.println("  $y, ${gaussFit[0]}, ${gaussFit[1]}, ${gaussFit[2]}")
                            }
                            centerMeans[y] = gaussFit[1]
                        }
                        centerX = Optional.of((centerMeans.sigmaClip().median() + 0.5).toInt())
                        println("Calculated centerX = ${centerX.get()}")
                        csvWriter?.let {
                            it.close()
                        }
                    }

                    if (!centerY.isPresent) {
                        val csvWriter = if (verboseMode) {
                            val file = inputFile.prefixName(outputDirectory, "find_center_y_${channel}_").suffixExtension(".csv")
                            println("Saving $file")
                            val csvWriter = PrintWriter(FileWriter(file))
                            csvWriter.println("  X, Amplitude, Mean, Sigma")
                            csvWriter
                        } else {
                            null
                        }
                        val centerMeans = DoubleArray(inputImage.width)
                        for (x in 0 until inputImage.width) {
                            val points = WeightedObservedPoints()
                            for (y in 0 until inputImage.height) {
                                val value = matrix[x, y]
                                if (value in low..high) {
                                    points.add(y.toDouble(), value)
                                }
                            }
                            val gaussFit = GaussianCurveFitter.create().fit(points.toList())
                            csvWriter?.let {
                                it.println("  $x, ${gaussFit[0]}, ${gaussFit[1]}, ${gaussFit[2]}")
                            }
                            centerMeans[x] = gaussFit[1]
                        }
                        centerY = Optional.of((centerMeans.sigmaClip().median() + 0.5).toInt())
                        println("Calculated centerY = ${centerY.get()}")
                        csvWriter?.let {
                            it.close()
                        }
                    }

                    val calculatedMaxDistance = centerX.get() + centerY.get() // TODO calculate better
                    val distanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }
                    val clippedDistanceValues = Array<MutableList<Float>>(calculatedMaxDistance) { mutableListOf() }

                    var maxDistance = 0
                    var clippedMaxDistance = 0
                    for (y in 0 until inputImage.height) {
                        for (x in 0 until inputImage.width) {
                            val value = matrix[x, y]
                            val dx = (centerX.get() - x).toDouble()
                            val dy = (centerY.get() - y).toDouble()
                            val distance = (sqrt(dx * dx + dy * dy) + 0.5).toInt()
                            distanceValues[distance].add(value.toFloat())
                            maxDistance = max(maxDistance, distance)
                            if (value in low..high) {
                                clippedDistanceValues[distance].add(value.toFloat())
                                clippedMaxDistance = max(maxDistance, distance)
                            }
                        }
                    }

                    val xValues = mutableListOf<Double>()
                    val yValues = mutableListOf<Double>()
                    for (i in 0 until clippedMaxDistance) {
                        val median = clippedDistanceValues[i].toFloatArray().medianInplace()
                        if (median.isFinite()) {
                            xValues.add(i.toDouble())
                            yValues.add(median.toDouble())

                            xValues.add(-i.toDouble())
                            yValues.add(median.toDouble())
                        }
                    }
                    println("Samples for regression analysis: ${xValues.size}")

                    // using apache
                    val points = WeightedObservedPoints()
                    for (i in xValues.indices) {
                        points.add(xValues[i], yValues[i])
                    }
                    val gaussFit = GaussianCurveFitter.create().fit(points.toList())
                    println("Gauss: ${gaussFit.contentToString()}")
                    val polynomialFit2 = PolynomialCurveFitter.create(2).fit(points.toList())
                    println("Polynomial: ${polynomialFit2.contentToString()}")

                    var errorGauss = 0.0
                    var errorPolynomial = 0.0
                    for (i in xValues.indices) {
                        val y1 = yValues[i]
                        val yGauss = gaussFunction(xValues[i], gaussFit[0], gaussFit[1], gaussFit[2])
                        val deltaGauss = y1 - yGauss
                        errorGauss += deltaGauss * deltaGauss
                        val yPolynomial = polynomialFunction(xValues[i], polynomialFit2)
                        val deltaPolynomial = y1 - yPolynomial
                        errorPolynomial += deltaPolynomial * deltaPolynomial
                    }
                    errorGauss /= xValues.size
                    errorPolynomial /= xValues.size
                    println("Standard Error (Gauss):      $errorGauss")
                    println("Standard Error (Polynomial): $errorPolynomial")
                    println()

                    if (debugMode) {
                        val file = inputFile.prefixName(outputDirectory, "vignette_curve_fit_${channel}_").suffixExtension(".csv")
                        println("Saving $file")
                        val csvWriter = PrintWriter(FileWriter(file))
                        csvWriter.println("  Index, Count, Average, Median, Polynomial2, Gauss")
                        for (i in 0 until maxDistance) {
                            val count = distanceValues[i].size
                            val average = distanceValues[i].toFloatArray().average().finiteOrElse()
                            val median = distanceValues[i].toFloatArray().medianInplace().finiteOrElse()
                            val polynomial2 = polynomialFunction(i.toDouble(), polynomialFit2)
                            val gauss = gaussFunction(i.toDouble(), gaussFit[0], gaussFit[1], gaussFit[2])
                            csvWriter.println("  $i, $count, $average, $median, $polynomial2, $gauss")
                        }
                        csvWriter.close()
                        println()
                    }

                    val flatMatrix = CalculatedMatrix(inputImage.height, inputImage.width) { x, y ->
                        val dx = (centerX.get() - x).toDouble()
                        val dy = (centerY.get() - y).toDouble()
                        val distance = sqrt(dx*dx + dy*dy)
                        when (model) {
                            "gauss" -> gaussFunction(distance, gaussFit[0], gaussFit[1], gaussFit[2])
                            "polynomial" -> polynomialFunction(distance, polynomialFit2)
                            "auto" -> if (errorGauss < errorPolynomial) {
                                gaussFunction(distance, gaussFit[0], gaussFit[1], gaussFit[2])
                            } else {
                                polynomialFunction(distance, polynomialFit2)
                            }
                            else -> throw IllegalArgumentException("Unknown model: $model")
                        }

                    }

                    val flatMax = flatMatrix.max()
                    val normalizedFlatMatrix = flatMatrix.copy().onEach { v -> v / flatMax }

                    channelMatrices.add(normalizedFlatMatrix)
                }

                var flatImage = MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to channelMatrices[0],
                    Channel.Green to channelMatrices[min(1, channelMatrices.size-1)],
                    Channel.Blue to channelMatrices[min(2, channelMatrices.size-1)])

                if (debugMode) {
                    val flatFile = inputFile.prefixName(outputDirectory, "flat_")
                    println("Saving $flatFile for manual analysis")
                    ImageWriter.write(flatImage, flatFile)
                    println()
                }

                inputImage / flatImage
            }
        }

    fun scriptRemoveVignetteExperimental(): Script =
        kimage(0.1) {
            name = "remove-vignette"
            title = "Remove vignette effect from image"
            description = """
                Calculates a statistical model of the vignette effect of the input image and removes it.
                """
            arguments {
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
                        val value = matrix[x, y]
                        val dx = (centerX - x).toDouble()
                        val dy = (centerY - y).toDouble()
                        val distance = sqrt(dx * dx + dy * dy).toInt()
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

                        x.add(-i.toFloat())
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

                val flatMatrix = CalculatedMatrix(inputImage.height, inputImage.width) { x, y ->
                    val dx = (centerX-x).toDouble()
                    val dy = (centerY-y).toDouble()
                    val distance = sqrt(dx*dx + dy*dy).toFloat()
                    polynomialFunction(distance.toFloat()).toDouble()
                    //cauchyFunction(distance)
                }

                var flatImage = MatrixImage(inputImage.width, inputImage.height,
                    Channel.Red to flatMatrix,
                    Channel.Green to flatMatrix,
                    Channel.Blue to flatMatrix)

                inputImage / flatImage * flatMatrix.max()
            }
        }

    fun scriptTestArgs(): Script =
        kimage(0.1) {
            name = "test-args"
            title = "Test script to show how to handle arguments in a kimage script"
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
                optionalDouble("optionalDoubleArg") {
                    description = "Example argument for an optional double value."
                    min = 0.0
                    max = 100.0
                }
                boolean("booleanArg") {
                    description = "Example argument for a boolean value."
                    default = false
                }
                optionalBoolean("optionalBooleanArg") {
                    description = "Example argument for am optional boolean value."
                }
                string("stringArg") {
                    description = "Example argument for a string value."
                    default = "undefined"
                }
                optionalString("optionalStringArg") {
                    description = "Example argument for an optional string value."
                }
                string("allowedStringArg") {
                    description = "Example argument for a string value with some allowed strings."
                    allowed = listOf("red", "green", "blue")
                    default = "red"
                }
                string("regexStringArg") {
                    description = """
                Example argument for a string value with regular expression.
                The input only allows `a` characters (at least one).
                """
                    regex = "a+"
                    default = "aaa"
                }
                file("fileArg") {
                    description = "Example argument for a file."
                    isFile = true
                    default = File("unknown.txt")
                }
                file("mandatoryFileArg") {
                    description = "Example argument for a file."
                    isFile = true
                }
                file("dirArg") {
                    description = "Example argument for a directory with default."
                    isDirectory = true
                    default = File(".")
                }
                file("mandatoryDirArg") {
                    description = "Example argument for a mandatory directory."
                    isDirectory = true
                }
                optionalFile("optionalFileArg") {
                    description = "Example argument for an optional file."
                    isFile = true
                }
                optionalFile("optionalDirArg") {
                    description = "Example argument for an optional directory."
                    isDirectory = true
                }
                point("pointArg") {
                    min = Point(0, 0)
                    max = Point(100.0, 200.0)
                    hint = Hint.ImageXY
                }
                optionalPoint("optionalPointArg") {
                    min = Point(1, 2)
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
                val optionalDoubleArg: Optional<Double> by arguments
                val booleanArg: Boolean by arguments
                val optionalBooleanArg: Optional<Boolean> by arguments
                val stringArg: String by arguments
                val optionalStringArg: Optional<String> by arguments
                val allowedStringArg: String by arguments
                val regexStringArg: String by arguments
                val fileArg: File by arguments
                val mandatoryFileArg: File by arguments
                val dirArg: File by arguments
                val mandatoryDirArg: File by arguments
                val optionalFileArg: Optional<File> by arguments
                val optionalDirArg: Optional<File> by arguments

                val listOfIntArg: List<Int> by arguments
                val optionalListOfIntArg: Optional<List<Int>> by arguments

                val recordArg: Map<String, Any> by arguments
                val recordInt: Int by recordArg
                val recordString: String by recordArg
                val recordDouble: Double by recordArg

                val optionalRecordArg: Optional<Map<String, Any>> by arguments

                println("Raw Arguments:")
                for (rawArgument in rawArguments) {
                    val key: String = rawArgument.key
                    val value: Any = rawArgument.value
                    println("  ${key} = ${value}")
                }
                println()

                println("Processed Arguments as Variables:")
                println("  intArg = $intArg")
                println("  optionalIntArg = $optionalIntArg")
                println("  doubleArg = $doubleArg")
                println("  optionalDoubleArg = $optionalDoubleArg")
                println("  booleanArg = $booleanArg")
                println("  optionalBooleanArg = $optionalBooleanArg")
                println("  stringArg = $stringArg")
                println("  optionalStringArg = $optionalStringArg")
                println("  allowedStringArg = $allowedStringArg")
                println("  regexStringArg = $regexStringArg")
                println("  fileArg = $fileArg")
                println("  mandatoryFileArg = $mandatoryFileArg")
                println("  dirArg = $dirArg")
                println("  mandatoryDirArg = $mandatoryDirArg")
                println("  optionalFileArg = $optionalFileArg")
                println("  optionalDirArg = $optionalDirArg")
                println("  listOfIntArg = $listOfIntArg")
                println("  optionalListOfIntArg = $optionalListOfIntArg")
                println("  recordArg = $recordArg")
                println("  recordInt = $recordInt")
                println("  recordString = $recordString")
                println("  recordDouble = $recordDouble")
                println("  optionalRecordArg = $optionalRecordArg")

                println("Input Files:")
                for (file in inputFiles) {
                    println("  File: $file exists=${file.exists()}")
                }
            }
        }

    fun scriptTestMulti(): Script =
        kimage(0.1) {
            name = "test-multi"
            title = "Test script to show how to process multiple images in a kimage script"
            description = """
                Example script as starting point for developers.
                """
            arguments {
                boolean("center") {
                    description = "Center images to fit the first image."
                    default = false
                }
            }

            // 'multi' means that all input files are processed together in a single run
            multi {
                // The processed arguments are available in a Map 'arguments'
                val center: Boolean by arguments // Use the kotlin delegate by feature to map the arguments into typed variables

                // Variables 'verboseMode' and 'debugMode' are automatically available
                if (verboseMode) {
                    println("arguments  = $arguments")

                    // The raw unprocessed arguments (no default values filled) are available in the rare case you need them
                    println("rawArguments  = $rawArguments")
                }

                // The input files can now be processed
                println("inputFiles  = $inputFiles")

                // Note: In 'multi' mode there are no 'inputFile' or 'inputImage' variables
                //       You need to load the images yourself and process them.
                //       Preloading all the input images would be more convenient but might lead to out-of-memory problems

                // The following processing code is a documented version of the 'stack-average' script:

                var stacked: Image? = null // The result image is initally null

                for (inputFile in inputFiles) {
                    println("Loading image: $inputFile")
                    val image = ImageReader.read(inputFile) // Load an input image (one by one - only the stacked image is kept in memory)
                    stacked = if (stacked == null) {
                        image // If the input image is the first image assign it to 'stacked'
                    } else {
                        // Ensure input image has the same size as 'stacked'
                        val croppedImage = if (center) {
                            image.cropCenter(stacked.width/2, stacked.height/2, stacked.width, stacked.height)
                        } else {
                            image.crop(0, 0, stacked.width, stacked.height)
                        }
                        stacked + croppedImage // Assign the pixel-wise sum of 'stacked' and 'croppedImage' to 'stacked'
                    }
                }

                // The last value in the script is the output
                if (stacked == null) {
                    null // If no image was processed return null
                } else {
                    stacked / inputFiles.size.toDouble() // Pixel-wise divide the stacked image by the number of input files -> average pixels
                }
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

        return ::abc
    }

    fun runScript(script: Script, vararg filepaths: String) {
        runScript(script, mapOf(), *filepaths)
    }

    fun runScript(script: Script, arguments: Map<String, String>, vararg filepaths: String) {
        runScript(script, arguments, filepaths.map { File(it) })
    }

    fun runScript(script: Script, arguments: Map<String, String>, files: List<File>) {
        KImageManager.executeScript(script, arguments, files, true, true, true , null, "output", ".")
        KImageManager.executeScript(script, arguments, files, false, true, true , null, "output", ".")
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