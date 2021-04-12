import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(inputMultiMode)

println("Background removal using median filter + gaussian blur")

val files = inputFiles as List<File>
val file = inputFile as File
val image = inputImage as Image
val parameters = inputParameters as Map<String, String>

println("Input file = $file")
println("Input image = $image")

val size = max(1, min(image.width, image.height) / 100)
//val size = 5
println("Kernel size = $size")

val background = image.medianFilter(size).gaussianBlurFilter(size * 2)
image - background * 0.99

