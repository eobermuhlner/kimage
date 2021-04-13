import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(multiMode)

fun findInterestingCropCenter(image: Image, radius: Int): Pair<Int, Int> {
    val insetWidth = image.width / 4
    val insetHeight = image.height / 4
    val radiusStep = radius / 10
    var bestStdDev = 0.0
    var bestX = 0
    var bestY = 0
    for (y in insetWidth until image.height - insetWidth step radiusStep) {
        for (x in insetHeight until image.width - insetHeight step radiusStep) {
            val croppedImage = image.cropCenter(x, y, radius, radius)
            val stddev = croppedImage[Channel.Red].stddev()
            if (stddev > bestStdDev) {
                ImageWriter.write(croppedImage, File("check_${stddev}.png"))
                //println("stddev $x, $y : $stddev")
                bestStdDev = stddev
                bestX = x
                bestY = y
            }
        }
    }
    return Pair(bestX, bestY)
}

println("Align multiple images and stack using average")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

val baseInputFile = files[0]
println("Loading base image: $baseInputFile")
val baseImage = ImageReader.readMatrixImage(baseInputFile)
println("Base image: $baseImage")

val checkRadius = parameters.getOrDefault("checkRadius", "100").toInt()
val searchRadius = parameters.getOrDefault("searchRadius", "200").toInt()

val (autoCenterX, autoCenterY) = findInterestingCropCenter(baseImage, checkRadius)

val centerX = parameters.getOrDefault("centerX", autoCenterX.toString()).toInt()
val centerY = parameters.getOrDefault("centerY", autoCenterY.toString()).toInt()

if (verboseMode) {
    println("Parameters:")
    println("  checkRadius = $checkRadius")
    println("  searchRadius = $searchRadius")
    println("  centerX = $centerX")
    println("  centerY = $centerY")
    println()
}

val imageAligner = SimpleImageAligner(checkRadius)

ImageWriter.write(baseImage.cropCenter(centerX, centerY, checkRadius), File("check_" + baseInputFile.name))

var sumImage: Image = MatrixImage(baseImage)

for (index in 0 until inputFiles.size) {
    val inputFile = files[index]
    println("Loading image: $inputFile")

    val image = ImageReader.readMatrixImage(inputFile)
    println("Aligning image: $image")

    val alignment = imageAligner.align(baseImage, image, centerX = centerX, centerY = centerY, maxOffset = searchRadius)
    println("Alignment: $alignment")

    val alignedImage = image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
    ImageWriter.write(alignedImage, File("aligned_" + inputFile.name))

    val delta = deltaRGB(baseImage, alignedImage)
    ImageWriter.write(delta, File("delta_aligned_" + inputFile.name))

    val error = baseImage.averageError(alignedImage)
    println("Standard error to base image: $inputFile $error")

    sumImage += alignedImage

    val stackedImage = sumImage / (index + 1).toDouble()
    ImageWriter.write(stackedImage, File("stacked_" + inputFile.name))
}

sumImage / inputFiles.size.toDouble()

