import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(inputSingleMode)

println("Test single image script")

val files = inputFiles as List<File>
val file = inputFile as File
val image = inputImage as Image
val parameters = inputParameters as Map<String, String>

println("Input files: $files")
println("Input file: $file")
println("Input image: $image")
for (parameter in parameters) {
    val key: String = parameter.key
    val value: String = parameter.value
    println("  Parameter: ${key} = ${value}")
}

null