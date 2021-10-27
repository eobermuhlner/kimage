import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "remove-background-gradient-manual3"
    title = "Remove the background by subtracting an interpolated gradient"
    description = """
                Calculates an interpolated background image from 3 manually chosen fix points and removes it.
                
                This script is useful for astrophotography if the fix points are chosen to represent the background.
                
                Use the --debug option to save intermediate images for manual analysis.
                """
    arguments {
        double("removePercent") {
            description = """
                        The percentage of the calculated background that will be removed.
                        """
            default = 100.0
        }
        int("x1") {
            hint = Hint.ImageX
        }
        int("y1") {
            hint = Hint.ImageY
        }
        int("x2") {
            hint = Hint.ImageX
        }
        int("y2") {
            hint = Hint.ImageY
        }
        int("x3") {
            hint = Hint.ImageX
        }
        int("y3") {
            hint = Hint.ImageY
        }
        int("medianRadius") {
            default  = 50
        }
    }
    single {
        val removePercent: Double by arguments
        val x1: Int by arguments
        val y1: Int by arguments
        val x2: Int by arguments
        val y2: Int by arguments
        val x3: Int by arguments
        val y3: Int by arguments
        val medianRadius: Int by arguments

        val points = listOf(x1 to y1, x2 to y2, x3 to y3)

        var backgroundImage = inputImage.interpolate(points, medianRadius = medianRadius)
        if (debugMode) {
            val backgroundFile = inputFile.prefixName(outputDirectory, "background_")
            println("Writing $backgroundFile")
            ImageWriter.write(backgroundImage, backgroundFile)
        }

        if (verboseMode) println("Subtracting $removePercent% background glow from original image ...")
        backgroundImage = backgroundImage * (removePercent/100.0)

        if (debugMode) {
            val deltaFile = inputFile.prefixName(outputDirectory, "delta_")
            println("Saving $deltaFile for manual analysis")
            val deltaImage = deltaChannel(inputImage, backgroundImage)
            ImageWriter.write(deltaImage, deltaFile)
        }

        inputImage - backgroundImage
    }
}
