import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

require(multiMode)

println("Test multi image script")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

println("Input files: $files")
for (parameter in parameters) {
    val key: String = parameter.key
    val value: String = parameter.value
    println("  Parameter: ${key} = ${value}")
}

for (file in files) {
    println("  File: $file ${file.exists()}")
}

null
