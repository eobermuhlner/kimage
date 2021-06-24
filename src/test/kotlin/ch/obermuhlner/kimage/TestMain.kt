package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.align.ImageAligner
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.math.*
import java.io.File
import kotlin.random.Random

object TestMain {
    @JvmStatic
    fun main(args: Array<String>) {
//        exampleFilters("lena512color.tiff")

//        exampleChannelManipulation("animal.png")
//        exampleFilters("animal.png")

//        exampleChannelManipulation("orion_32bit.tif")
//        exampleFilters("orion_32bit.tif")

//        exampleImages()
//        exampleMedianExperiments()

        exampleError()
        //exampleAlign()
    }

    private fun exampleError() {
        var image1 = measureElapsed("Read image1") {
            //ImageReader.read(File("images/align/IMG_7130.TIF"))
            ImageReader.readMatrixImage(File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2528.TIF"))
        }
        
        val radius = 50

        val (bestX, bestY) = measureElapsed("findInterestingCrop") {
            findInterestingCrop(image1, radius)
        }

        val inputFiles = listOf(
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2528.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2530.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2531.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2532.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2533.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2534.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2535.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2536.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2537.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2538.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2539.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2540.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2541.TIF"),
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
            MatrixImage(image1.cropCenter(centerX, centerY, largeRadiusX, largeRadiusY))
        }
        val otherImage = measureElapsed("Crop other image") {
            MatrixImage(image2.cropCenter(centerX, centerY, largeRadiusX, largeRadiusY))
        }

        ImageWriter.write(baseImage, File("${name}_partial_base.png"))
        ImageWriter.write(otherImage, File("${name}_partial_other.png"))

        var bestError = 1.0
        var bestX = 0
        var bestY = 0

        val croppedBaseImage = baseImage.cropCenter(baseImage.width/2, baseImage.height/2, radiusX, radiusY, false)
        ImageWriter.write(croppedBaseImage, File("${name}_cropped_base.png"))
        println("Base $croppedBaseImage")
        val errorImage = MatrixImage(searchWidth, searchHeight)

        for (dy in -searchRadiusY .. searchRadiusY) {
            for (dx in -searchRadiusX .. searchRadiusX) {
                val croppedOtherImage = otherImage.cropCenter(otherImage.width/2+dx, otherImage.height/2+dy, radiusX, radiusY, false)
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

        ImageWriter.write(otherImage.cropCenter(otherImage.width/2+bestX, otherImage.height/2+bestY, radiusX, radiusY, false), File("${name}_cropped_other.png"))

        val min = errorImage[Channel.Red].min()
        val max = errorImage[Channel.Red].max()
        println("min: $min")
        println("max: $max")

        ImageWriter.write(errorImage, File("${name}_error.png"))
        ImageWriter.write(errorImage / max, File("${name}_stretched_error.png"))

        return Pair(bestX, bestY)
    }

    private fun exampleAlign() {
//        val inputFiles = listOf(
//            File("images/lena512color.tiff"),
//            File("images/lena512color.tiff"),
//            File("images/output/lena512color.tiff_median_10_square.png"))

//        val inputFiles = listOf(
//            File("images/align/IMG_7130.TIF"),
//            File("images/align/IMG_7131.TIF"),
//            File("images/align/IMG_7132.TIF"),
//            File("images/align/IMG_7133.TIF"),
//            File("images/align/IMG_7134.TIF"),
//        )

        val inputFiles = listOf(
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2528.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2530.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2531.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2532.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2533.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2534.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2535.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2536.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2537.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2538.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2539.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2540.TIF"),
            File("C:/Users/EricObermuhlner/Pictures/Astrophotography/2021-03-29 M42/Moon/TIFF16/IMG_2541.TIF"),
        )

        val radius = 100
        val imageAligner = ImageAligner(radius)

        println("Base image: ${inputFiles[0]}")
        val baseImage = ImageReader.readMatrixImage(inputFiles[0])

        val (centerX, centerY) = findInterestingCrop(baseImage, radius)

        var sumImage: Image = MatrixImage(baseImage)

        for (index in 1 until inputFiles.size) {
            val inputFile = inputFiles[index]

            println("Stack image: ${inputFile}")
            val stackImage = ImageReader.readMatrixImage(inputFile)

            val alignment = measureElapsed("align $inputFile") {
                imageAligner.align(baseImage, stackImage, centerX, centerY, maxOffset = 500)
            }
            println(alignment)

            val img = stackImage.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
            val error = baseImage.averageError(img)
            println("Image error: $error")

            val delta = deltaRGB(baseImage, img)
            ImageWriter.write(delta, File("delta_aligned_" + inputFile.name))

            sumImage += stackImage

            val stackedImage = sumImage / (index + 1).toDouble()
            ImageWriter.write(stackedImage, File("stacked_" + inputFile.name))
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
//            "orion_32bit.tif",
//            "orion_small_compress0.png",
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