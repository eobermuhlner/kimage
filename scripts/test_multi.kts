import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.File
import kotlin.math.min
import kotlin.math.max

require(inputMultiMode)

println("Test multi image script")

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

println("Input files = $files")
for (parameter in parameters) {
    println("  Parameter ${parameter.key} = ${parameter.value}")
}

for (file in files) {
    println("  $file ${file.exists()}")
}

null
