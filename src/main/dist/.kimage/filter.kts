import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

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
    }

    single {
        val filter: String by arguments
        val radius: Int by arguments
        val strength: Double by arguments

        when (filter) {
            "blur" -> inputImage.gaussianBlurFilter(radius)
            "median" -> inputImage.medianFilter(radius)
            "average" -> inputImage.averageFilter(radius)
            "sharpen" -> inputImage.sharpenFilter()
            "unsharpMask" -> inputImage.unsharpMaskFilter(radius, strength)
            "unsharpMask3x3" -> inputImage.kernelFilter(KernelFilter.UnsharpMask)
            "blur3x3" -> inputImage.kernelFilter(KernelFilter.GaussianBlur3)
            "blur5x5" -> inputImage.kernelFilter(KernelFilter.GaussianBlur5)
            "blur7x7" -> inputImage.kernelFilter(KernelFilter.GaussianBlur7)
            "edgeDetectionStrong3x3" -> inputImage.edgeDetectionStrongFilter()
            "edgeDetectionCross3x3" -> inputImage.edgeDetectionCrossFilter()
            "edgeDetectionDiagonal3x3" -> inputImage.edgeDetectionDiagonalFilter()
            "sobel3x3" -> inputImage.sobelFilter3()
            "sobel5x5" -> inputImage.sobelFilter5()
            else -> throw IllegalArgumentException("Unknown filter: $filter")
        }
    }
}
