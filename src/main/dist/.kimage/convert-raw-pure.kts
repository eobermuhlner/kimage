import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.util.*
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.math.*

kimage(0.1) {
    name = "convert-raw"
    title = "Convert an image from pure raw format into tiff"
    description = """
                Convert a raw image with minimal transformations into a tiff image.
                """
    arguments {
        string("dcraw") {
            default = "dcraw"
        }
        string("interpolation") {
            allowed = listOf("none", "none-unscaled", "none-uncropped")
            default = "none-uncropped"
        }
    }

    fun dcraw(
        dcraw: String,
        interpolation: String,
        file: File
    ) {
        val processBuilder = ProcessBuilder()

        val command = mutableListOf(dcraw, "-T", "-v")

        when(interpolation) {
            "none" -> command.add("-d")
            "none-unscaled" -> command.add("-D")
            "none-uncropped" -> command.add("-E")
        }

        command.add("-4")
        command.add("-j")
        command.add("-t")
        command.add("0")
        command.add(file.path)

        println("Command: $command")

        processBuilder.command(command)

        val process = processBuilder.start()

        Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
        val exitCode = process.waitFor()
        println("Exit code: $exitCode")
    }

    multi {
        val dcraw: String by arguments
        val interpolation: String by arguments

        for (inputFile in inputFiles) {
            println("Converting $inputFile")
            dcraw(dcraw, interpolation, inputFile)
            println()
        }

        null
    }
}
