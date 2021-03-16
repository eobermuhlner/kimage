package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import java.io.File

object TestMain {
    @JvmStatic
    fun main(args: Array<String>) {
//        exampleChannelManipulation("animal.png")
        exampleFilters("animal.png")

//        exampleChannelManipulation("orion_32bit.tif")
//        exampleFilters("orion_32bit.tif")

        exampleImages()
        exampleExperiments()
    }

    private fun exampleExperiments() {
        val image = ImageReader.read(File("images/animal.png"))

        example("gaussian_blur_3") {
            image.gaussianBlur(3)
        }

        example("channel_gray") {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Gray to image[Channel.Gray])
        }
    }

    fun exampleImages() {
        for (imageName in listOf("animal.png", "orion_32bit.tif", "orion_small_compress0.png")) {
            example("read_$imageName", imageName) {
                ImageReader.read(File("images/$imageName"))
            }
            example("read_matrix_$imageName", imageName) {
                ImageReader.readMatrixImage(File("images/$imageName"))
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
        example("median_10_circle", imageName) {
            MedianFilter(10, Shape.Circle).filter(image)
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
            image.croppedImage(width3, height3, width3, height3, true)
        }

        example("cropped_nonstrict", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            image.croppedImage(width3, height3, width3, height3, false)
        }

        example("average_3_square_cropped", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            val filtered = AverageFilter(3, Shape.Square).filter(image)
            filtered.croppedImage(width3, height3, width3, height3)
        }

        example("cropped_strict_average_3_square", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            val cropped = image.croppedImage(width3, height3, width3, height3)
            AverageFilter(3, Shape.Square).filter(cropped)
        }

        example("cropped_nonstrict_average_3_square", imageName) {
            val width3 = image.width / 3
            val height3 = image.height / 3
            val cropped = image.croppedImage(width3, height3, width3, height3, false)
            AverageFilter(3, Shape.Square).filter(cropped)
        }
    }

    private fun example(name: String, imageName: String = "untitled.png", func: () -> Image) {
        val image = measureElapsed(name) {
            func()
        }
        if (name.endsWith(".tif") || name.endsWith(".jpg") || name.endsWith(".png")) {
            ImageWriter.write(image, File("images/output/${imageName}_$name"))
        } else {
            ImageWriter.write(image, File("images/output/${imageName}_$name.png"))
        }
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