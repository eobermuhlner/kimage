import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.nio.file.*
import kotlin.math.*

kimage(0.1) {
    name = "info"
    title = "Print info about images"
    arguments {
    }

    multi {
        println(String.format("%-40s %6s %6s %12s %5s %5s %8s %8s", "Name", "Exists", "Type", "Bytes", "Width", "Height", "Median", "Stddev"))
        for (file in inputFiles) {
            val fileSize = if (file.exists()) Files.size(file.toPath()) else 0
            val fileType = if (file.isFile()) "File" else if (file.isDirectory()) "Dir" else "Other"

            print(String.format("%-40s %6s %6s %12d", file.name, file.exists(), fileType, fileSize))
            if (file.isFile()) {
                try {
                    val image = ImageReader.read(file)

                    print(
                        String.format(
                            "%5d %5d %8.5f %8.5f",
                            image.width,
                            image.height,
                            image.values().median(),
                            image.values().stddev()
                        )
                    )
                } catch (ex: Exception) {
                    // ignore
                }
            }
            println()
        }
    }
}


