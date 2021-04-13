import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(multiMode)

println("Align multiple images and stack using average")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

val checkRadius = parameters.getOrDefault("checkRadius", "100").toInt()
val searchRadius = parameters.getOrDefault("searchRadius", "200").toInt()

if (verboseMode) {
    println("Parameters:")
    println("  checkRadius = $checkRadius")
    println("  searchRadius = $searchRadius")
}

val imageAligner = SimpleImageAligner(checkRadius)

println("Loading base image: ${files[0]}")
val baseImage = ImageReader.readMatrixImage(files[0])
println("Base image: $baseImage")

var sumImage: Image = MatrixImage(baseImage)

for (index in 1 until inputFiles.size) {
    val inputFile = files[index]
    println("Loading image: $inputFile")

    val stackImage = ImageReader.readMatrixImage(inputFile)
    println("Aligning image: $stackImage")

    val alignment = imageAligner.align(baseImage, stackImage, maxOffset = searchRadius)
    println("Alignment: $alignment")

    val img = stackImage.crop(alignment.x, alignment.y, baseImage.width, baseImage.height)
    ImageWriter.write(img, File("aligned_" + inputFile.name))

    val delta = deltaRGB(baseImage, img)
    ImageWriter.write(delta, File("delta_aligned_" + inputFile.name))

    sumImage += stackImage

    val stackedImage = sumImage / (index + 1).toDouble()
    ImageWriter.write(stackedImage, File("stacked_" + inputFile.name))
}

sumImage / inputFiles.size.toDouble()

