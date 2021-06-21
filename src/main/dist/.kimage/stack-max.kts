import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(multiMode)

println("Stack multiple images using max")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

var sumImage: Image = ImageReader.readMatrixImage(files[0])

for (index in 1 until files.size) {
    val inputFile = files[index]

    println("Loading image: $inputFile")

    val image = ImageReader.readMatrixImage(inputFile)

    sumImage = sumImage max image
}

sumImage
