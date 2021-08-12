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
    name = "move-file"
    title = "Move image files into another directory"
    description = """
                Move images.
                """
    arguments {
        file("target") {
            description = """
                The target directory to move the image file into.
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
            println("Moving $inputFile into $target directory")
            Files.move(inputFile.toPath(), File(target, inputFile.name).toPath())
        }
    }
}
