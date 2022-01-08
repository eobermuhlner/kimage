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
    name = "statistics"
    title = "Print statistical info about images"
    arguments {
    }

    multi {
        println(String.format("%-40s %8s %8s %8s %8s %8s %8s", "Name", "Min", "Max", "Median", "Stddev", "NormMedian", "NormStddev"))
        for (file in inputFiles) {
            val fileSize = if (file.exists()) Files.size(file.toPath()) else 0
            val fileType = if (file.isFile()) "File" else if (file.isDirectory()) "Dir" else "Other"

            print(String.format("%-40s ", file.name))
            if (file.isFile()) {
                try {
                    val image = ImageReader.read(file)

                    val min = image.values().min()
                    val max = image.values().max()
                    val median = image.values().median()
                    val stddev = image.values().stddev()

                    val normalizedImage = image / (median / 0.5)

                    val normalizedMedian = normalizedImage.values().median()
                    val normalizedStddev = normalizedImage.values().stddev()

                    print(
                        String.format(
                            "%8.5f %8.5f %8.5f %8.5f %8.5f %8.5f",
                            min,
                            max,
                            median,
                            stddev,
                            normalizedMedian,
                            normalizedStddev
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
