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
            allowed = listOf("blur", "median", "average", "unsharpMask", "edgeDetectionStrong", "edgeDetectionCross", "edgeDetectionDiagonal")
        }
        int("radius") {
            min = 0
            default = 3
        }
    }

    single {
        val filter: String by arguments
        val radius: Int by arguments

        when (filter) {
            "blur" -> inputImage.gaussianBlurFilter(3)
            "median" -> inputImage.medianFilter(3)
            "average" -> inputImage.averageFilter(3)
            "sharpen" -> inputImage.sharpenFilter()
            "unsharpMask" -> inputImage.unsharpMaskFilter()
            "edgeDetectionStrong" -> inputImage.edgeDetectionStrongFilter()
            "edgeDetectionCross" -> inputImage.edgeDetectionCrossFilter()
            "edgeDetectionDiagonal" -> inputImage.edgeDetectionDiagonalFilter()
            else -> throw IllegalArgumentException("Unknown filter: $filter")
        }
    }
}
