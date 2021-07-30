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
                - `median` uses a median filter of the specified `radius`
                - `average` uses an average filter of the specified `radius`
                - `sharpen` uses a 3x3 sharpen mask
                - `unsharpMask` uses a 3x3 unsharp mask
                - `edgeDetectionStrong` detects edges along the horizontal/vertial axes and both diagonals using a 3x3 kernel
                - `edgeDetectionCross` detects edges along the horizontal/vertial axes using a 3x3 kernel
                - `edgeDetectionDiagonal` detects edges along both diagonals using a 3x3 kernel
                """
            allowed = listOf("blur", "median", "average", "sharpen", "unsharpMask", "edgeDetectionStrong", "edgeDetectionCross", "edgeDetectionDiagonal")
        }
        int("radius") {
            description = """
                The radius in pixels for the `blur`, `median` and `average` filters.
                """
            enabledWhen = Reference("filter").isEqual("blur", "median", "average")
            min = 1
            default = 3
        }
    }

    single {
        val filter: String by arguments
        val radius: Int by arguments

        when (filter) {
            "blur" -> inputImage.gaussianBlurFilter(radius)
            "median" -> inputImage.medianFilter(radius)
            "average" -> inputImage.averageFilter(radius)
            "sharpen" -> inputImage.sharpenFilter()
            "unsharpMask" -> inputImage.unsharpMaskFilter()
            "edgeDetectionStrong" -> inputImage.edgeDetectionStrongFilter()
            "edgeDetectionCross" -> inputImage.edgeDetectionCrossFilter()
            "edgeDetectionDiagonal" -> inputImage.edgeDetectionDiagonalFilter()
            else -> throw IllegalArgumentException("Unknown filter: $filter")
        }
    }
}
