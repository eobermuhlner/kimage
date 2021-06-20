import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.nio.file.*
import kotlin.math.*

require(multiMode)

val files = inputFiles as List<File>
val parameters = inputParameters as Map<String, String>

println(String.format("%-40s %6s %6s %12s", "Name", "Exists", "Type", "Bytes"))
for (file in files) {
    val fileSize = if (file.exists()) Files.size(file.toPath()) else 0
    val fileType = if (file.isFile()) "File" else if (file.isDirectory()) "Dir" else "Other"
    println(String.format("%-40s %6s %6s %12d", file.name, file.exists(), fileType, fileSize))
}


