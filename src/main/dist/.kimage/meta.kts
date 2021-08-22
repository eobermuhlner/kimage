import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.nio.file.*
import kotlin.math.*
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.common.GenericImageMetadata

kimage(0.1) {
    name = "meta"
    title = "Print meta info about images"
    arguments {
    }

    multi {
        for (inputFile in inputFiles) {
            if (!inputFile.isFile()) {
                continue;
            }

            val fileSize = if (inputFile.exists()) Files.size(inputFile.toPath()) else 0

            println("File: ${inputFile.name}")
            println("Size: $fileSize")

            val metadata = Imaging.getMetadata(inputFile)
            if (metadata != null) {
                for (item in metadata.items) {
                    when (item) {
                        is GenericImageMetadata.GenericImageMetadataItem -> {
                            println("${item.keyword} : ${item.text}")
                        }
                        else -> {
                            println("${item}")
                        }
                    }
                }
            }
            println()
        }
    }
}


