import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "remove-background-median"
    description = """
                Removes the background from the input image by subtracting a blurred median of the input.
                
                This script is useful for astrophotography if the image contains mainly stars and not too much nebulas.
                The size of the median filter can be increased to remove stars and nebulas completely.
                
                Use the --debug option to save intermediate images for manual analysis.
                """
    arguments {
        double("removePercent") {
            description = """
                        The percentage of the calculated background that will be removed.
                        """
            default = 99.0
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

        println("Arguments:")
        println("  removePercent = $removePercent%")
        println("  medianFilterPercent = $medianFilterPercent%")
        println("  blurFilterPercent = $blurFilterPercent%")
        println("  medianFilterSize = $medianFilterSize")
        println("  blurFilterSize = $blurFilterSize")

        if (verboseMode) println("Running median filter ...")
        val medianImage = inputImage.medianFilter(medianFilterSize)
        if (debugMode) {
            val medianFile = File("median_" + inputFile.name)
            println("Writing $medianFile")
            ImageWriter.write(medianImage, medianFile)
        }

        if (verboseMode) println("Running gaussian blur filter ...")
        val backgroundImage = medianImage.gaussianBlurFilter(blurFilterSize)
        if (debugMode) {
            val backgroundFile = File("background_" + inputFile.name)
            println("Writing $backgroundFile")
            ImageWriter.write(backgroundImage, backgroundFile)
        }

        if (verboseMode) println("Subtracting $removePercent% background glow from original image ...")
        inputImage - backgroundImage * (removePercent/100.0)
    }
}
