import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.nio.file.Files
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "copy-file"
    title = "Copy image files into another directory"
    description = """
                Copy images.
                """
    arguments {
        file("target") {
            description = """
                The target directory to copy the image file into.
                """
            isDirectory = true
        }
    }

    multi {
        val target: File by arguments

        if (!target.exists()) {
            target.mkdirs()
        }

        for (inputFile in inputFiles) {
            println("Copying $inputFile into $target directory")
            Files.copy(inputFile.toPath(), File(target, inputFile.name).toPath())
        }
    }
}
