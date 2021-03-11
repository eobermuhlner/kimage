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
        //exampleFilters()

        exampleChannelManipulation()
    }

    fun exampleChannelManipulation() {
        val image = ImageReader.readMatrixImage(File("images/animal.png"))

        example("channel_red") {
            MatrixImageFilter({ _, matrix -> matrix }, { channel -> channel == Channel.Red }).filter(image)
        }

        example("channel_swap_red_blue") {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to image.getMatrix(Channel.Blue),
                    Channel.Green to image.getMatrix(Channel.Green),
                    Channel.Blue to image.getMatrix(Channel.Red))
        }

        example("channel_blur_green") {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to image.getMatrix(Channel.Red),
                    Channel.Green to GaussianBlurFilter.blur(image.getMatrix(Channel.Green), 3),
                    Channel.Blue to image.getMatrix(Channel.Blue))
        }

        example("channel_reduce_red") {
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to image.getMatrix(Channel.Red) * 0.8,
                    Channel.Green to image.getMatrix(Channel.Green),
                    Channel.Blue to image.getMatrix(Channel.Blue))
        }

        example("channel_gray") {
            val gray = image.getMatrix(Channel.Red) / 3.0 + image.getMatrix(Channel.Green) / 3.0 + image.getMatrix(Channel.Blue) / 3.0
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to gray,
                    Channel.Green to gray,
                    Channel.Blue to gray)
        }

        example("channel_luminance") {
            val luminance = image.getMatrix(Channel.Red) * 0.2126 + image.getMatrix(Channel.Green) * 0.7152 + image.getMatrix(Channel.Blue) * 0.0722
            MatrixImage(
                    image.width,
                    image.height,
                    Channel.Red to luminance,
                    Channel.Green to luminance,
                    Channel.Blue to luminance)
        }

    }

    fun exampleFilters() {
        //val image = ImageReader.readMatrixImage(File("images/Autosave001_small_compress0.png"))
        val image = ImageReader.readMatrixImage(File("images/animal.png"))

        example("noop") {
            image
        }
        example("copy") {
            CopyFilter().filter(image)
        }
        example("gaussian_blur_1") {
            GaussianBlurFilter(1).filter(image)
        }
        example("gaussian_blur_3") {
            GaussianBlurFilter(3).filter(image)
        }
        example("gaussian_blur_10") {
            GaussianBlurFilter(10).filter(image)
        }

        example("median_10_horizontal") {
            MedianFilter(10, Shape.Horizontal).filter(image)
        }
        example("median_10_vertical") {
            MedianFilter(10, Shape.Vertical).filter(image)
        }
        example("median_10_horizontal_vertical") {
            val tmp = MedianFilter(10, Shape.Horizontal).filter(image)
            MedianFilter(10, Shape.Vertical).filter(tmp)
        }
        example("median_10_vertical_horizontal") {
            val tmp = MedianFilter(10, Shape.Vertical).filter(image)
            MedianFilter(10, Shape.Horizontal).filter(tmp)
        }
        example("median_10_cross") {
            MedianFilter(10, Shape.Cross).filter(image)
        }
        example("median_10_diagonalcross") {
            MedianFilter(10, Shape.DiagonalCross).filter(image)
        }
        example("median_10_star") {
            MedianFilter(10, Shape.Star).filter(image)
        }
        example("median_10_square") {
            MedianFilter(10, Shape.Square).filter(image)
        }
        example("median_10_circle") {
            MedianFilter(10, Shape.Circle).filter(image)
        }

        example("average_10_horizontal") {
            AverageFilter(10, Shape.Horizontal).filter(image)
        }
        example("average_10_vertical") {
            AverageFilter(10, Shape.Vertical).filter(image)
        }
        example("average_10_horizontal_vertical") {
            val tmp = AverageFilter(10, Shape.Horizontal).filter(image)
            AverageFilter(10, Shape.Vertical).filter(tmp)
        }
        example("average_10_vertical_horizontal") {
            val tmp = AverageFilter(10, Shape.Vertical).filter(image)
            AverageFilter(10, Shape.Horizontal).filter(tmp)
        }
        example("average_10_cross") {
            AverageFilter(10, Shape.Cross).filter(image)
        }
        example("average_10_diagonalcross") {
            AverageFilter(10, Shape.DiagonalCross).filter(image)
        }
        example("average_10_star") {
            AverageFilter(10, Shape.Star).filter(image)
        }
        example("average_10_square") {
            AverageFilter(10, Shape.Square).filter(image)
        }
        example("average_10_circle") {
            AverageFilter(10, Shape.Circle).filter(image)
        }

        example("kernel_edgedetectionstrong") {
            KernelFilter(KernelFilter.EdgeDetectionStrong).filter(image)
        }
        example("kernel_edgedetectioncross") {
            KernelFilter(KernelFilter.EdgeDetectionCross).filter(image)
        }
        example("kernel_edgedetectiondiagonal") {
            KernelFilter(KernelFilter.EdgeDetectionDiagonal).filter(image)
        }
        example("kernel_sharpen") {
            KernelFilter(KernelFilter.Sharpen).filter(image)
        }
        example("kernel_unsharpmask") {
            KernelFilter(KernelFilter.UnsharpMask).filter(image)
        }
        example("kernel_gaussianblur3") {
            KernelFilter(KernelFilter.GaussianBlur3).filter(image)
        }
        example("kernel_gaussianblur5") {
            KernelFilter(KernelFilter.GaussianBlur5).filter(image)
        }
    }

    private fun example(name: String, func: () -> Image) {
        val image = measureElapsed(name) {
            func()
        }
        ImageWriter.write(image, File("images/output_$name.png"))
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