package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.File
import kotlin.random.Random

object TestMain {
    @JvmStatic
    fun main(args: Array<String>) {
//        exampleFilters("lena512color.tiff")
//
//        exampleChannelManipulation("animal.png")
//        exampleFilters("animal.png")
//
//        exampleChannelManipulation("orion.png")
//        exampleFilters("orion.png")
//
//        exampleImages()
//        exampleMedianExperiments()
//
//        exampleScale("lena512color.tiff")

        //exampleError()
        exampleAlign()
    }

    private fun exampleScale(imageName: String) {
        val image = ImageReader.readMatrixImage(File("images/$imageName"))

        for (scaling in Scaling.values()) {
            example("scaled_50%_$scaling", imageName) {
                image.scaleBy(0.5, 0.5, scaling)
            }
            example("scaled_200%_$scaling", imageName) {
                image.scaleBy(2.0, 2.0, scaling)
            }
            example("scaled_1000%_$scaling", imageName) {
                image.cropCenter(image.width / 10, image.width/2, image.height/2).scaleBy(10.0, 10.0, scaling)
            }
            example("scaled_12345%_$scaling", imageName) {
                image.cropCenter(image.width / 100, image.width/2, image.height/2).scaleBy(123.45, 123.45, scaling)
            }
        }
    }

    private fun exampleError() {
        var image1 = measureElapsed("Read image1") {
            ImageReader.readMatrixImage(File("images/align/orion1.png"))
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
                ImageReader.readMatrixImage(inputFile)
            }

            val (alignX, alignY) = measureElapsed("alignImages") {
                alignImages(inputFile.name, image1, image2, radius, radius, 200, 200, bestX, bestY)
            }

            val alignedImage2 = image2.crop(alignX, alignY, image2.width, image2.height)
            ImageWriter.write(alignedImage2, File("aligned_" + inputFile.name))

            sumImage += alignedImage2
            sumImageCount++

            val stackedImage = sumImage / sumImageCount.toDouble()
            ImageWriter.write(stackedImage, File("stacked_" + inputFile.name))
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
                val stddev = croppedImage[Channel.Red].stddev()
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

        val min = errorImage[Channel.Red].min()
        val max = errorImage[Channel.Red].max()
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
        val baseImage = ImageReader.readMatrixImage(inputFiles[0]).scaleBy(scaleFactor, scaleFactor)

        val (centerX, centerY) = imageAligner.findInterestingCropCenter(baseImage)

        for (index in 1 until inputFiles.size) {
            val inputFile = inputFiles[index]

            println("Align image: ${inputFile}")
            val stackImage = ImageReader.readMatrixImage(inputFile).scaleBy(scaleFactor, scaleFactor)

            val alignment = measureElapsed("align $inputFile") {
                imageAligner.align(baseImage, stackImage, centerX, centerY, maxOffset = 30)
            }
            println(alignment)

            val alignedImage = stackImage.crop(alignment.x, alignment.y, baseImage.width, baseImage.height, false)
            val error = baseImage.averageError(alignedImage)
            println("Image error: $error")

            ImageWriter.write(alignedImage, File("images/output/aligned_" + inputFile.name))

            val delta = deltaRGB(baseImage, alignedImage)
            ImageWriter.write(delta, File("images/output/delta_aligned_" + inputFile.name))
        }
    }

    private fun exampleMedianExperiments() {
        val image = ImageReader.readMatrixImage(File("images/lena512color.tiff"))
        val gimp_median = ImageReader.readMatrixImage(File("images/lena512color_gimp_median3.tiff"))

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
        example("delta_median_to_gimp_median_rgb") {
            deltaRGB(median, gimp_median)
        }
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
            "lena512color.tiff",
            "animal.png",
//            "orion1.png",
        )) {
            example("read_write_$imageName", imageName) {
                ImageReader.read(File("images/$imageName"))
            }
            example("read_write_matrix_$imageName", imageName) {
                ImageReader.readMatrixImage(File("images/$imageName"))
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

        val image = ImageReader.readMatrixImage(File("images/$imageName"))
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