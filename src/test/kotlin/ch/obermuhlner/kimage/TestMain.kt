package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.fft.Complex
import ch.obermuhlner.kimage.fft.ComplexMatrix
import ch.obermuhlner.kimage.fft.FFT
import ch.obermuhlner.kimage.fft.contentToString
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.io.ImageReader.read
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.Matrix
import ch.obermuhlner.kimage.matrix.contentToString
import java.io.File
import java.util.*
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

object TestMain {
    @JvmStatic
    fun main(args: Array<String>) {
//        exampleFilters("lena512.tiff")
//
//        exampleChannelManipulation("animal.png")
//        exampleFilters("animal.png")
//
//        exampleChannelManipulation("lena512.png")
//        exampleFilters("lena512.png")
//
//        exampleImages()
//        exampleMedianExperiments()
//
//        exampleScale("lena512.tiff")
//
//        exampleInterpolate("colors.png")
//        exampleInterpolate("lena512.tiff")
//
//        exampleError()
//        exampleAlign()

        //exampleFFT("animal.png")

        //exampleConvoluteFFT()
        //exampleDeconvoluteFFT()

        //exampleStatistics()
        exampleDenoise()
    }

    private fun exampleConvoluteFFT() {
        val originalMatrix = Matrix.matrixOf(7, 7)
        originalMatrix[3, 3] = 1.0

        val convolutedMatrix = originalMatrix.convolute(KernelFilter.GaussianBlur3)
        println("convoluted =")
        println(convolutedMatrix.contentToString(true))

        val paddedMatrix = FFT.padPowerOfTwo(originalMatrix)

        val kernelMatrix = KernelFilter.GaussianBlur3
        val paddedKernel = Matrix.matrixOf(paddedMatrix.width, paddedMatrix.height)
        paddedKernel.set(kernelMatrix, 0, 0)
        println("paddedKernel =")
        println(paddedKernel.contentToString(true))

        val frequencyMatrix = FFT.fft(ComplexMatrix(paddedMatrix))
        println("frequencyMatrix =")
        println(frequencyMatrix.contentToString(true))

        val frequencyKernel = FFT.fft(ComplexMatrix(paddedKernel))
        println("frequencyKernel =")
        println(frequencyMatrix.contentToString(true))

        val frequencyResult = frequencyMatrix elementTimes frequencyKernel
        println("frequencyResult =")
        println(frequencyResult.contentToString(true))

        val result = FFT.fftInverse(frequencyResult).re
        result.onEach { v -> if (abs(v) < 1E-10) 0.0 else v }
        println("result =")
        println(result.contentToString(true))
    }

    private fun exampleDeconvoluteFFT() {
        val originalMatrix = Matrix.matrixOf(7, 7)
        originalMatrix[3, 3] = 1.0

        val convolutedMatrix = originalMatrix.convolute(KernelFilter.GaussianBlur3)
        println("convoluted =")
        println(convolutedMatrix.contentToString(true))

        val paddedMatrix = FFT.padPowerOfTwo(convolutedMatrix)

        val kernelMatrix = KernelFilter.GaussianBlur3
        val paddedKernel = Matrix.matrixOf(paddedMatrix.width, paddedMatrix.height)
        paddedKernel.set(kernelMatrix, 0, 0)
        println("paddedKernel =")
        println(paddedKernel.contentToString(true))

        val frequencyMatrix = FFT.fft(ComplexMatrix(paddedMatrix))
        println("frequencyMatrix =")
        println(frequencyMatrix.contentToString(true))

        val frequencyKernel = FFT.fft(ComplexMatrix(paddedKernel))
        println("frequencyKernel =")
        println(frequencyMatrix.contentToString(true))
        frequencyKernel.onEach { c ->
            var re = c.re
            var im = c.im
            if (re == 0.0) {
                re = 1.0E-100
            }
            if (im == 0.0) {
                im = 1.0E-100
            }
            Complex(re, im)
        }

        val frequencyDeconvoluted = frequencyMatrix elementDiv frequencyKernel
        println("frequencyDeconvoluted =")
        println(frequencyDeconvoluted.contentToString(true))

        val deconvolutedMatrix = FFT.fftInverse(frequencyDeconvoluted).re
        println("deconvolutedMatrix =")
        println(deconvolutedMatrix.contentToString(true))
    }

    private fun exampleFFT(imageName: String) {
        val image = read(File("images/$imageName"))
        val matrix = image[Channel.Gray]
        val frequencyMatrix = FFT.fft(ComplexMatrix(FFT.padPowerOfTwo(matrix)))

        val matrixRestored = FFT.fftInverse(frequencyMatrix)
        val matrixRestoredRe = matrixRestored.re
        val imageRestored = MatrixImage(matrixRestoredRe.width, matrixRestoredRe.height,
            Channel.Red to matrixRestoredRe,
            Channel.Green to matrixRestoredRe,
            Channel.Blue to matrixRestoredRe)
        ImageWriter.write(imageRestored, File("FFT_restored.png"))

        val frequencyMatrixRe = frequencyMatrix.re
        val frequencyMatrixIm = frequencyMatrix.im
        frequencyMatrixRe -= frequencyMatrixRe.min()
        frequencyMatrixRe /= frequencyMatrixRe.max()
        frequencyMatrixRe.onEach { v -> Math.log(v + 1.0) }

        frequencyMatrixIm -= frequencyMatrixIm.min()
        frequencyMatrixIm /= frequencyMatrixIm.max()

        val imageRe = MatrixImage(frequencyMatrixRe.width, frequencyMatrixRe.height,
            Channel.Red to frequencyMatrixRe,
            Channel.Green to frequencyMatrixRe,
            Channel.Blue to frequencyMatrixRe)
        ImageWriter.write(imageRe, File("FFT_re.png"))

        val imageIm = MatrixImage(frequencyMatrixIm.width, frequencyMatrixIm.height,
            Channel.Red to frequencyMatrixIm,
            Channel.Green to frequencyMatrixIm,
            Channel.Blue to frequencyMatrixIm)
        ImageWriter.write(imageIm, File("FFT_im.png"))
    }

    private fun exampleInterpolate(imageName: String) {
        val image = read(File("images/$imageName"))

        val inset = 50
        // estimated nice power values for n points
        // 2 -> 2.0
        // 3 -> 8.0
        // 4 -> 10.0
        // 5 -> 15.0
        // 9 -> 30.0
        // 25 -> 100.0

        val points2 = listOf(
            inset to inset,
            image.width - inset to image.height - inset
        )

        val points3 = listOf(
            image.width / 2 to inset,
            inset to image.height - inset,
            image.width - inset to image.height - inset
        )

        val points4 = listOf(
            inset to inset,
            image.width - inset to inset,
            inset to image.height - inset,
            image.width - inset to image.height - inset
        )

        val points5 = listOf(
            image.width / 2 to image.height / 2,
            inset to inset,
            image.width - inset to inset,
            inset to image.height - inset,
            image.width - inset to image.height - inset
        )

        val grid3x3 = pointGrid(image, 3, 3)
        val grid5x5 = pointGrid(image, 5, 5)
        val grid10x10 = pointGrid(image, 10, 10)

        example("interpolate_2", imageName) {
            image.interpolate(points2)
        }
        example("interpolate_2_subtract", imageName) {
            image - image.interpolate(points2)
        }
        example("interpolate_3", imageName) {
            image.interpolate(points3)
        }
        example("interpolate_3_subtract", imageName) {
            image - image.interpolate(points3)
        }
        example("interpolate_4", imageName) {
            image.interpolate(points4)
        }
        example("interpolate_4_subtract", imageName) {
            image - image.interpolate(points4)
        }
        example("interpolate_5", imageName) {
            image.interpolate(points5)
        }
        example("interpolate_5_subtract", imageName) {
            image - image.interpolate(points5)
        }
        example("interpolate_3x3", imageName) {
            image.interpolate(grid3x3)
        }
        example("interpolate_3x3_subtract", imageName) {
            image - image.interpolate(grid3x3)
        }
        example("interpolate_5x5", imageName) {
            image.interpolate(grid5x5)
        }
        example("interpolate_5x5_subtract", imageName) {
            image - image.interpolate(grid5x5)
        }
        example("interpolate_10x10", imageName) {
            image.interpolate(grid10x10)
        }
        example("interpolate_10x10_subtract", imageName) {
            image - image.interpolate(grid10x10)
        }
        example("interpolate_clipped5x5", imageName) {
            image.interpolate(sigmaClipPointGrid(image, grid5x5))
        }
        example("interpolate_clipped5x5_subtract", imageName) {
            image - image.interpolate(sigmaClipPointGrid(image, grid5x5))
        }
        example("interpolate_clipped5x5_delta", imageName) {
            deltaChannel(image, image.interpolate(sigmaClipPointGrid(image, grid5x5)))
        }
        example("interpolate_clipped10x10", imageName) {
            image.interpolate(sigmaClipPointGrid(image, grid10x10))
        }
        example("interpolate_clipped10x10_subtract", imageName) {
            image - image.interpolate(sigmaClipPointGrid(image, grid10x10))
        }

    }

    private fun pointGrid(image: Image, xCount: Int, yCount: Int): List<Pair<Int, Int>> {
        val grid = mutableListOf<Pair<Int, Int>>()
        val width = image.width
        val height = image.height
        for (x in 0 until xCount) {
            for (y in 0 until yCount) {
                val xCenter = (width.toDouble() / xCount * (x + 0.5)).toInt()
                val yCenter = (height.toDouble() / yCount * (y + 0.5)).toInt()
                grid.add(Pair(xCenter, yCenter))
            }
        }
        return grid
    }

    private fun sigmaClipPointGrid(image: Image, grid: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val gridWithMedian = grid.map {
            val median = image.cropCenter(100, it.first, it.second).values().fastMedian()
            Pair(it, median)
        }
        val gridMedian = gridWithMedian.map { it.second }.median()
        val gridSigma = gridWithMedian.map { it.second }.stddev()

        val low = gridMedian - gridSigma * 0.5
        val high = gridMedian + gridSigma * 0.5

        return gridWithMedian.filter { it.second in low..high } .map { it.first }
    }

    private fun exampleScale(imageName: String) {
        val image = read(File("images/$imageName"))

        for (scaling in Scaling.values()) {
            example("scaled_50%_$scaling", imageName) {
                image.scaleBy(0.5, 0.5, 0.0, 0.0, scaling)
            }
            example("scaled_200%_$scaling", imageName) {
                image.scaleBy(2.0, 2.0, 0.0, 0.0, scaling)
            }
            example("scaled_1000%_$scaling", imageName) {
                image.cropCenter(image.width / 10, image.width / 2, image.height / 2).scaleBy(
                    10.0,
                    10.0,
                    0.0,
                    0.0,
                    scaling
                )
            }
            example("scaled_12345%_$scaling", imageName) {
                image.cropCenter(image.width / 100, image.width / 2, image.height / 2).scaleBy(
                    123.45,
                    123.45,
                    0.0,
                    0.0,
                    scaling
                )
            }
        }
    }

    private fun exampleError() {
        val outputDirectory = File(".")
        var image1 = measureElapsed("Read image1") {
            read(File("images/align/orion1.png"))
        }
        
        val radius = 50

        val (bestX, bestY) = measureElapsed("findInterestingCrop") {
            findInterestingCrop(image1, radius)
        }

        val inputFiles = listOf(
            File("images/align/orion1.png"),
            File("images/align/orion1.png")
        )

        var sumImage: Image = MatrixImage(image1)
        var sumImageCount: Int = 1

        for (inputFile in inputFiles) {
            val image2 = measureElapsed("Read image2 $inputFile") {
                read(inputFile)
            }

            val (alignX, alignY) = measureElapsed("alignImages") {
                alignImages(inputFile.name, image1, image2, radius, radius, 200, 200, bestX, bestY)
            }

            val alignedImage2 = image2.crop(alignX, alignY, image2.width, image2.height)
            ImageWriter.write(alignedImage2, inputFile.prefixName(outputDirectory, "aligned_"))

            sumImage += alignedImage2
            sumImageCount++

            val stackedImage = sumImage / sumImageCount.toDouble()
            ImageWriter.write(stackedImage, inputFile.prefixName(outputDirectory, "stacked_"))
        }
    }

    fun findInterestingCrop(image: Image, radius: Int): Pair<Int, Int> {
        val insetWidth = image.width / 4
        val insetHeight = image.height / 4
        var bestStdDev = 0.0
        var bestX = 0
        var bestY = 0
        for (y in insetWidth until image.height - insetWidth step radius) {
            for (x in insetHeight until image.width - insetHeight step radius) {
                val croppedImage = image.crop(x, y, radius, radius)
                val stddev = croppedImage.values().stddev()
                if (stddev > bestStdDev) {
                    //println("stddev $x, $y : $stddev")
                    bestStdDev = stddev
                    bestX = x
                    bestY = y
                }
            }
        }
        return Pair(bestX, bestY)
    }

    private fun alignImages(name: String, image1: Image, image2: Image, radius: Int, searchRadius: Int, centerX: Int = image1.width / 2, centerY: Int = image2.width) {
        alignImages(name, image1, image2, radius, radius, searchRadius, searchRadius, centerX, centerY)
    }

    private fun alignImages(name: String, image1: Image, image2: Image, radiusX: Int, radiusY: Int, searchRadiusX: Int, searchRadiusY: Int, centerX: Int = image1.width / 2, centerY: Int = image2.width): Pair<Int, Int> {
        val searchWidth = searchRadiusX * 2 + 1
        val searchHeight = searchRadiusY * 2 + 1
        val largeRadiusX = radiusX + searchRadiusX
        val largeRadiusY = radiusY + searchRadiusY

        val baseImage = measureElapsed("Crop base image") {
            MatrixImage(image1.cropCenter(largeRadiusX, largeRadiusY, centerX, centerY))
        }
        val otherImage = measureElapsed("Crop other image") {
            MatrixImage(image2.cropCenter(largeRadiusX, largeRadiusY, centerX, centerY))
        }

        ImageWriter.write(baseImage, File("${name}_partial_base.png"))
        ImageWriter.write(otherImage, File("${name}_partial_other.png"))

        var bestError = 1.0
        var bestX = 0
        var bestY = 0

        val croppedBaseImage = baseImage.cropCenter(radiusX, radiusY, baseImage.width/2, baseImage.height/2, false)
        ImageWriter.write(croppedBaseImage, File("${name}_cropped_base.png"))
        println("Base $croppedBaseImage")
        val errorImage = MatrixImage(searchWidth, searchHeight)

        for (dy in -searchRadiusY .. searchRadiusY) {
            for (dx in -searchRadiusX .. searchRadiusX) {
                val croppedOtherImage = otherImage.cropCenter(
                    radiusX,
                    radiusY,
                    otherImage.width/2+dx,
                    otherImage.height/2+dy,
                    false
                )
                val error = croppedBaseImage.averageError(croppedOtherImage)
                if (error < bestError) {
                    println("$dx, $dy : $error")
                    bestError = error
                    bestX = dx
                    bestY = dy
                }
                val x = searchRadiusX + dx
                val y = searchRadiusY + dy
                errorImage.setPixel(x, y, Channel.Red, error)
                errorImage.setPixel(x, y, Channel.Green, error)
                errorImage.setPixel(x, y, Channel.Blue, error)
            }
        }

        ImageWriter.write(otherImage.cropCenter(
            radiusX,
            radiusY,
            otherImage.width/2+bestX,
            otherImage.height/2+bestY,
            false
        ), File("${name}_cropped_other.png"))

        val min = errorImage.values().min()
        val max = errorImage.values().max()
        println("min: $min")
        println("max: $max")

        ImageWriter.write(errorImage, File("${name}_error.png"))
        ImageWriter.write(errorImage / max, File("${name}_stretched_error.png"))

        return Pair(bestX, bestY)
    }

    private fun exampleAlign() {

        val inputFiles = listOf(
            File("images/align/orion1.png"),
            File("images/align/orion2.png")
        )

        val scaleFactor = 1.0

        val radius = 10
        val imageAligner = ImageAligner(radius)

        println("Base image: ${inputFiles[0]}")
        val baseImage = read(inputFiles[0]).scaleBy(scaleFactor, scaleFactor)

        val (centerX, centerY) = imageAligner.findInterestingCropCenter(baseImage)

        for (index in 1 until inputFiles.size) {
            val inputFile = inputFiles[index]

            println("Align image: ${inputFile}")
            val stackImage = read(inputFile).scaleBy(scaleFactor, scaleFactor)

            val alignment = measureElapsed("align $inputFile") {
                imageAligner.align(baseImage, stackImage, centerX, centerY, maxOffset = 30)
            }
            println(alignment)

            val alignedImage = stackImage.crop(alignment.x, alignment.y, baseImage.width, baseImage.height, false)
            val error = baseImage.averageError(alignedImage)
            println("Image error: $error")

            ImageWriter.write(alignedImage, File("images/output/aligned_" + inputFile.name))

            val delta = deltaChannel(baseImage, alignedImage)
            ImageWriter.write(delta, File("images/output/delta_aligned_" + inputFile.name))
        }
    }

    private fun exampleMedianExperiments() {
        val image = read(File("images/lena512.tiff"))
        //val gimp_median = read(File("images/lena512color_gimp_median3.tiff"))

        example("noise") {
            randomNoise(image, 0.1)
            image
        }

        val median = example("median_3") {
            MedianFilter(3).filter(image)
        }
        val fast_median = example("fast_median_3") {
            FastMedianFilter(3).filter(image)
        }
        val fast_median_pixel = example("fast_median_pixel_3") {
            FastMedianPixelFilter(3).filter(image)
        }
        example("delta_image_to_median_rgb") {
            deltaRGB(image, median)
        }
        example("delta_image_to_median_lum") {
            deltaChannel(image, median)
        }
        example("delta_image_to_fast_median_rgb") {
            deltaRGB(image, fast_median)
        }
//        example("delta_median_to_gimp_median_rgb") {
//            deltaRGB(median, gimp_median)
//        }
        example("delta_median_to_fast_median_rgb") {
            deltaRGB(median, fast_median)
        }
        example("delta_median_to_fast_median_lum") {
            deltaChannel(median, fast_median)
        }
        example("delta_median_to_fast_median_red") {
            deltaChannel(median, fast_median, channel = Channel.Red)
        }
        example("delta_median_to_fast_median_green") {
            deltaChannel(median, fast_median, channel = Channel.Green)
        }
        example("delta_median_to_fast_median_blue") {
            deltaChannel(median, fast_median, channel = Channel.Blue)
        }
        example("delta_median_to_fast_median_pixel_rgb") {
            deltaRGB(median, fast_median_pixel)
        }

//        example("channel_gray") {
//            MatrixImage(
//                    image.width,
//                    image.height,
//                    Channel.Gray to image[Channel.Gray])
//        }
    }

    private fun randomNoise(image: Image, noise: Double) {
        val random = Random(1)

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (random.nextDouble() < noise) {
                    image[x, y, Channel.Red] = 0.0 //random.nextDouble()
                    image[x, y, Channel.Green] = 1.0 //random.nextDouble()
                    image[x, y, Channel.Blue] = 0.0 //random.nextDouble()
                }
            }
        }
    }

    fun exampleImages() {
        for (imageName in listOf(
            "lena512.tiff",
            "animal.png",
//            "orion1.png",
        )) {
            example("read_write_$imageName", imageName) {
                ImageReader.read(File("images/$imageName"))
            }
            example("read_write_matrix_$imageName", imageName) {
                read(File("images/$imageName"))
            }
            example("read_write_${imageName}.png", imageName) {
                ImageReader.read(File("images/$imageName"))
            }
            example("read_write_${imageName}.tif", imageName) {
                ImageReader.read(File("images/$imageName"))
            }
        }
    }

    fun exampleChannelManipulation(imageName: String) {
        println("### Example Filter: $imageName")

        val image = ImageReader.read(File("images/$imageName"))
        println("Image: $imageName ${image.width} x ${image.height}")

        example("channel_red", imageName) {
            MatrixImageFilter({ _, matrix -> matrix }, { channel -> channel == Channel.Red }).filter(image)
        }

        example("channel_green", imageName) {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Green to image[Channel.Green])
        }

        example("channel_blue", imageName) {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Blue to image[Channel.Blue])
        }

        example("channel_hue", imageName) {
            val hue = image[Channel.Hue]
            MatrixImage(
                image.width,
                image.height,
                Channel.Red to hue,
                Channel.Green to hue,
                Channel.Blue to hue)
        }

        example("channel_saturation", imageName) {
            val saturation = image[Channel.Saturation]
            MatrixImage(
                image.width,
                image.height,
                Channel.Red to saturation,
                Channel.Green to saturation,
                Channel.Blue to saturation)
        }

        example("channel_value", imageName) {
            val value = image[Channel.Brightness]
            MatrixImage(
                image.width,
                image.height,
                Channel.Red to value,
                Channel.Green to value,
                Channel.Blue to value)
        }

        example("channel_swap_red_blue", imageName) {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to image[Channel.Blue],
                    Channel.Green to image[Channel.Green],
                    Channel.Blue to image[Channel.Red])
        }

        example("channel_blur_green", imageName) {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to image[Channel.Red],
                    Channel.Green to GaussianBlurFilter.blur(image[Channel.Green], 3),
                    Channel.Blue to image[Channel.Blue])
        }

        example("channel_reduce_red", imageName) {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to image[Channel.Red] * 0.8,
                    Channel.Green to image[Channel.Green],
                    Channel.Blue to image[Channel.Blue])
        }

        example("channel_gray", imageName) {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Gray to image[Channel.Gray])
        }

        example("channel_gray_manual", imageName) {
            val gray = image[Channel.Red] / 3.0 + image[Channel.Green] / 3.0 + image[Channel.Blue] / 3.0
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to gray,
                    Channel.Green to gray,
                    Channel.Blue to gray)
        }

        example("channel_luminance", imageName) {
            val luminance = image[Channel.Red] * 0.2126 + image[Channel.Green] * 0.7152 + image[Channel.Blue] * 0.0722
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to luminance,
                    Channel.Green to luminance,
                    Channel.Blue to luminance)
        }

    }

    fun exampleFilters(imageName: String) {
        println("### Example Filter: $imageName")

        val image = read(File("images/$imageName"))
        println("Image: $imageName ${image.width} x ${image.height}")

        example("noop", imageName) {
            image
        }
        example("copy", imageName) {
            CopyFilter().filter(image)
        }
        example("gaussian_blur_1", imageName) {
            GaussianBlurFilter(1).filter(image)
        }
        example("gaussian_blur_3", imageName) {
            GaussianBlurFilter(3).filter(image)
        }
        example("gaussian_blur_10", imageName) {
            GaussianBlurFilter(10).filter(image)
        }

        example("median_10_horizontal", imageName) {
            MedianFilter(10, Shape.Horizontal).filter(image)
        }
        example("median_10_vertical", imageName) {
            MedianFilter(10, Shape.Vertical).filter(image)
        }
        example("median_10_horizontal_vertical", imageName) {
            val tmp = MedianFilter(10, Shape.Horizontal).filter(image)
            MedianFilter(10, Shape.Vertical).filter(tmp)
        }
        example("median_10_vertical_horizontal", imageName) {
            val tmp = MedianFilter(10, Shape.Vertical).filter(image)
            MedianFilter(10, Shape.Horizontal).filter(tmp)
        }
        example("median_10_cross", imageName) {
            MedianFilter(10, Shape.Cross).filter(image)
        }
        example("median_10_diagonalcross", imageName) {
            MedianFilter(10, Shape.DiagonalCross).filter(image)
        }
        example("median_10_cross_diagonalcross", imageName) {
            val tmp = MedianFilter(10, Shape.Cross).filter(image)
            MedianFilter(10, Shape.DiagonalCross).filter(tmp)
        }
        example("median_10_star", imageName) {
            MedianFilter(10, Shape.Star).filter(image)
        }
        example("median_10_square", imageName) {
            MedianFilter(10, Shape.Square).filter(image)
        }
        example("median_10_square_recursive", imageName) {
            MedianFilter(10, Shape.Square, true).filter(image)
        }
        example("median_10_circle", imageName) {
            MedianFilter(10, Shape.Circle).filter(image)
        }

        example("fast_median_10", imageName) {
            FastMedianFilter(10).filter(image)
        }
        example("fast_median_10_recursive", imageName) {
            FastMedianFilter(10, true).filter(image)
        }
        example("fast_median_pixel_10", imageName) {
            FastMedianPixelFilter(10).filter(image)
        }

        example("average_10_horizontal", imageName) {
            AverageFilter(10, Shape.Horizontal).filter(image)
        }
        example("average_10_vertical", imageName) {
            AverageFilter(10, Shape.Vertical).filter(image)
        }
        example("average_10_horizontal_vertical", imageName) {
            val tmp = AverageFilter(10, Shape.Horizontal).filter(image)
            AverageFilter(10, Shape.Vertical).filter(tmp)
        }
        example("average_10_vertical_horizontal", imageName) {
            val tmp = AverageFilter(10, Shape.Vertical).filter(image)
            AverageFilter(10, Shape.Horizontal).filter(tmp)
        }
        example("average_10_cross", imageName) {
            AverageFilter(10, Shape.Cross).filter(image)
        }
        example("average_10_diagonalcross", imageName) {
            AverageFilter(10, Shape.DiagonalCross).filter(image)
        }
        example("average_10_star", imageName) {
            AverageFilter(10, Shape.Star).filter(image)
        }
        example("average_10_square", imageName) {
            AverageFilter(10, Shape.Square).filter(image)
        }
        example("average_10_circle", imageName) {
            AverageFilter(10, Shape.Circle).filter(image)
        }

        example("kernel_edgedetectionstrong", imageName) {
            KernelFilter(KernelFilter.EdgeDetectionStrong).filter(image)
        }
        example("kernel_edgedetectioncross", imageName) {
            KernelFilter(KernelFilter.EdgeDetectionCross).filter(image)
        }
        example("kernel_edgedetectiondiagonal", imageName) {
            KernelFilter(KernelFilter.EdgeDetectionDiagonal).filter(image)
        }
        example("kernel_sharpen", imageName) {
            KernelFilter(KernelFilter.Sharpen).filter(image)
        }
        example("kernel_unsharpmask", imageName) {
            KernelFilter(KernelFilter.UnsharpMask).filter(image)
        }
        example("kernel_gaussianblur3", imageName) {
            KernelFilter(KernelFilter.GaussianBlur3).filter(image)
        }
        example("kernel_gaussianblur5", imageName) {
            KernelFilter(KernelFilter.GaussianBlur5).filter(image)
        }

        example("cropped_strict", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            image.crop(width3, height3, width3, height3, true)
        }

        example("cropped_nonstrict", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            image.crop(width3, height3, width3, height3, false)
        }

        example("average_3_square_cropped", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            val filtered = AverageFilter(3, Shape.Square).filter(image)
            filtered.crop(width3, height3, width3, height3)
        }

        example("cropped_strict_average_3_square", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            val cropped = image.crop(width3, height3, width3, height3)
            AverageFilter(3, Shape.Square).filter(cropped)
        }

        example("cropped_nonstrict_average_3_square", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            val cropped = image.crop(width3, height3, width3, height3, false)
            AverageFilter(3, Shape.Square).filter(cropped)
        }
    }

    fun DoubleArray.weightedMedian(): Double {
        val median = this.median()
        val sigma = this.stddev()
        return this.weightedAverage({ _, v ->
            val x = abs(v - median) / sigma
            1 / (sqrt(x + 1))
        })
    }
    private fun exampleStatistics() {
        val arr = createRandomArray(Random(), 5)
        val kappa = 2.0

        println("Average: ${arr.average()}")
        println("Median:  ${arr.median()}")
        println("Stddev:  ${arr.stddev()}")

        println("SigmaClipMedian: ${arr.sigmaClip(kappa).median()}")
        println("SigmaClipAverage: ${arr.sigmaClip(kappa).average()}")
        println("SigmaWinsorizeMedian: ${arr.sigmaWinsorize(kappa).median()}")
        println("SigmaWinsorizeAverage: ${arr.sigmaWinsorize(kappa).average()}")
        println("WeightedMedian: ${arr.weightedMedian()}")
        println("SigmaClipWeightedMedian: ${arr.sigmaClip(kappa).weightedMedian()}")
    }

    private fun exampleDenoise() {
        val kappa = 2.0

        denoise("median") { arr -> arr.median() }
        denoise("average") { arr -> arr.average() }
        denoise("weightedMedian") { arr -> arr.weightedMedian() }
        denoise("sigmaClip-median") { arr -> arr.sigmaClip(kappa).median() }
        denoise("sigmaClip-weightedMedian") { arr -> arr.sigmaClip(kappa).weightedMedian() }
        denoise("sigmaWinsorize-median") { arr -> arr.sigmaWinsorize(kappa).median() }
        denoise("sigmaWinsorize-weightedMedian") { arr -> arr.sigmaWinsorize(kappa).weightedMedian() }
    }

    private fun denoise(name: String, func: (DoubleArray) -> Double) {
        val random = Random()
        val output = mutableListOf<Double>()
        val nValues = 10000

        for (i in 0 until nValues) {
            val arr = createRandomArray(random, 100, 1)
            val value = func(arr)
            output += value
        }

        println("Denoise $nValues values using: $name")
        println("Average: ${output.average()}")
        println("Median:  ${output.median()}")
        println("Stddev:  ${output.stddev()}")
        println()
    }

    private fun createRandomArray(
        random: Random,
        n: Int = 5,
        nOutliers: Int = 1,
        randomBase: Double = 100.0,
        randomStddev: Double = 10.0,
        outlier: Double = 5555.0): DoubleArray {

        val arr = DoubleArray(n) { i ->
            if (i < nOutliers) {
                outlier
            } else {
                random.nextGaussian() * randomStddev + randomBase
            }
        }
        return arr
    }

    private fun example(name: String, imageName: String = "untitled.png", func: () -> Image): Image {
        val image = measureElapsed(name) {
            func()
        }
        if (name.endsWith(".tif") || name.endsWith(".tiff") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
            ImageWriter.write(image, File("images/output/${imageName}_$name"))
        } else {
            ImageWriter.write(image, File("images/output/${imageName}_$name.png"))
        }
        return image
    }

    fun <T> measureElapsed(name: String, func: () -> T): T {
        val startMillis = System.currentTimeMillis()
        val result = func.invoke()
        val endMillis = System.currentTimeMillis()
        val deltaMillis = endMillis - startMillis

        println("$name in $deltaMillis ms")
        return result
    }
}