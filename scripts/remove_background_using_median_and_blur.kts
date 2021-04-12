import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(singleMode)

println("Background removal using median filter + gaussian blur")

val files = inputFiles as List<File>
val file = inputFile as File
val image = inputImage as Image
val parameters = inputParameters as Map<String, String>

val removalFactor = parameters.getOrDefault("removalFactor", "0.99").toDouble()
val medianKernelPercent = parameters.getOrDefault("medianKernelPercent", "1").toDouble()
val blurKernelPercent = parameters.getOrDefault("blurKernelPercent", "2").toDouble()

if (verboseMode) {
    println("Parameters:")
    println("  removalFactor = $removalFactor")
    println("  medianKernelPercent = $medianKernelPercent")
    println("  blurKernelPercent = $blurKernelPercent")
}

val medianKernelSize = max(1, (min(image.width, image.height) * medianKernelPercent / 100.0).toInt())
val blurKernelSize = max(1, (min(image.width, image.height) * blurKernelPercent / 100.0).toInt())

if (verboseMode) {
    println("  -> calculated medianKernelSize = $medianKernelSize pixels")
    println("  -> calculated blurKernelSize = $blurKernelSize pixels")
}

val background = image.medianFilter(medianKernelSize).gaussianBlurFilter(blurKernelSize)
image - background * removalFactor

