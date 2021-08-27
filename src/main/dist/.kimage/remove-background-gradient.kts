import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import kotlin.math.*

fun pointGrid(width: Int, height: Int, xCount: Int, yCount: Int): List<Pair<Int, Int>> {
    val grid = mutableListOf<Pair<Int, Int>>()
    for (x in 0 until xCount) {
        for (y in 0 until yCount) {
            val xCenter = (width.toDouble() / xCount * (x + 0.5)).toInt()
            val yCenter = (height.toDouble() / yCount * (y + 0.5)).toInt()
            grid.add(Pair(xCenter, yCenter))
        }
    }
    return grid
}

fun sigmaClipPointGrid(image: Image, grid: List<Pair<Int, Int>>, kappa: Double = 0.5): List<Pair<Int, Int>> {
    val gridWithMedian = grid.map {
        val median = image.cropCenter(100, it.first, it.second).values().fastMedian()
        Pair(it, median)
    }
    val gridMedian = gridWithMedian.map { it.second }.median()
    val gridSigma = gridWithMedian.map { it.second }.stddev()

    val low = gridMedian - gridSigma * kappa
    val high = gridMedian + gridSigma * kappa

    return gridWithMedian.filter { it.second in low..high } .map { it.first }
}


kimage(0.1) {
    name = "remove-background-gradient"
    title = "Remove the background by subtracting an interpolated gradient"
    description = """
                Calculates an interpolated background image from several fix points and removes it.
                
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
        int("gridSize") {
            description = """
                        The size of the grid in the x and y axis.
                        The number of grid points is the square of the `gridSize`.
                        """
            default = 5
        }
        double("kappa") {
            description = """
                        The kappa factor is used in sigma-clipping of the grid to ignore grid points that do not contain enough background.
                        """
            default = 0.5
        }
    }
    single {
        val removePercent: Double by arguments
        val gridSize: Int by arguments
        val kappa: Double by arguments

        val grid = pointGrid(inputImage.width, inputImage.height, gridSize, gridSize)
        val clippedGrid = sigmaClipPointGrid(inputImage, grid, kappa)
        if (verboseMode) {
            println("The ${grid.size} points of the grid have been reduced to ${clippedGrid.size} by sigma-clipping")
        }

        var backgroundImage = inputImage.interpolate(clippedGrid)
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
