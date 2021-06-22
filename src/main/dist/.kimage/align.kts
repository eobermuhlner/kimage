import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(multiMode)

println("Align multiple images")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

val baseInputFile = files[0]
println("Loading base image: $baseInputFile")
val baseImage = ImageReader.readMatrixImage(baseInputFile)
println("Base image: $baseImage")
println()

val defaultCheckRadius = min(baseImage.width, baseImage.height) / 100
val defaultSearchRadius = defaultCheckRadius * 5

val checkRadius = parameters.getOrDefault("checkRadius", defaultCheckRadius.toString()).toInt()
val searchRadius = parameters.getOrDefault("searchRadius", defaultSearchRadius.toString()).toInt()

val imageAligner = ImageAligner(checkRadius)
val (autoCenterX, autoCenterY) = imageAligner.findInterestingCropCenter(baseImage)

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


//ImageWriter.write(baseImage.cropCenter(centerX, centerY, checkRadius), File("check_" + baseInputFile.name))

for (inputFile in files) {
    println("Loading image: $inputFile")

    val image = ImageReader.readMatrixImage(inputFile)
    println("Aligning image: $image")

    val alignment = imageAligner.align(baseImage, image, centerX = centerX, centerY = centerY, maxOffset = searchRadius)
    println("Alignment: $alignment")

    val alignedImage = image.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
    ImageWriter.write(alignedImage, File("aligned_" + inputFile.name))

    //val delta = deltaRGB(baseImage, alignedImage)
    //ImageWriter.write(delta, File("delta_aligned_" + inputFile.name))

    val error = baseImage.averageError(alignedImage)
    println("Standard error to base image: $inputFile $error")
    println()
}