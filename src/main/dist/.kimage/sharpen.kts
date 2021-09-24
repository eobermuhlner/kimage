import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import kotlin.math.*

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

