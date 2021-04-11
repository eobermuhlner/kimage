import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(inputSingleMode)

println("Align multiple images and average")
println("Input files = $inputFiles")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

val radius = 100
val imageAligner = SimpleImageAligner(radius)

println("Base image: ${files[0]}")
val baseImage = ImageReader.readMatrixImage(files[0])

var sumImage: Image = MatrixImage(baseImage)

for (index in 1 until inputFiles.size) {
    val inputFile = files[index]

    val stackImage = ImageReader.readMatrixImage(inputFile)

    val alignment = imageAligner.align(baseImage, stackImage, maxOffset = 200)
    println(alignment)

    val img = stackImage.croppedImage(alignment.x, alignment.y, baseImage.width, baseImage.height)
    val delta = deltaRGB(baseImage, img)
    ImageWriter.write(delta, File("delta_aligned_" + inputFile.name))

    sumImage += stackImage

    val stackedImage = sumImage / (index + 1).toDouble()
    ImageWriter.write(stackedImage, File("stacked_" + inputFile.name))
}

sumImage / inputFiles.size.toDouble()

