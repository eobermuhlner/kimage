import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.nio.file.*
import kotlin.math.*

kimage(0.1) {
    name = "info"
    description = """
                Print info about images.
                """
    arguments {
    }

    multi {
        println(String.format("%-40s %6s %6s %12s", "Name", "Exists", "Type", "Bytes"))
        for (file in inputFiles) {
            val fileSize = if (file.exists()) Files.size(file.toPath()) else 0
            val fileType = if (file.isFile()) "File" else if (file.isDirectory()) "Dir" else "Other"
            println(String.format("%-40s %6s %6s %12d", file.name, file.exists(), fileType, fileSize))
        }
    }
}


