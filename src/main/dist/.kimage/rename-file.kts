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
    name = "rename-file"
    title = "Copy and rename files into another directory"
    description = """
                Rename and renumber images.
                """
    arguments {
        string("name") {
            description = """
                The name of the renamed file.
                The index of the renamed file will be appended at the end of the filename.
                """
        }
        int("start") {
            description = """
                The target directory to move the image file into.
                """
            default = 1
        }
    }

    multi {
        val name: String by arguments
        val start: Int by arguments

        var index = start
        for (inputFile in inputFiles) {
            val outputFileName = name + "%04d".format(index) + "." + inputFile.extension
            println("Renaming $inputFile into $outputFileName")
            val outputFile = File(inputFile.parentFile, outputFileName)
            Files.copy(inputFile.toPath(), outputFile.toPath())

            index++
        }
    }
}
