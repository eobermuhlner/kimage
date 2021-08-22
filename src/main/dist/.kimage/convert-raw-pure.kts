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
    name = "convert-raw-pure"
    title = "Convert an image from pure raw format into tiff"
    description = """
                Convert a raw image with minimal transformations into a tiff image.
                """
    arguments {
        string("dcraw") {
            description = """
               The `dcraw` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
            default = "dcraw"
        }
        string("option") {
            description = """
                Specifies the transformation options to use.
                No demosaicing interpolation will be used.
                
                - `scaled`, `none`: No interpolation, with automatic scaling to fill the value range.
                
                  Corresponds to the `-d` option in the `dcraw` command line tool.
                - `unscaled`, `none-unscaled`: No interpolation, no scaling.
                
                  Corresponds to the `-D` option in the `dcraw` command line tool.
                - `uncropped`, `none-uncropped`: No interpolation, no scaling, no cropping.
                
                  Corresponds to the `-E` option in the `dcraw` command line tool.
                """
            allowed = listOf("scaled", "unscaled", "uncropped", "none", "none-unscaled", "none-uncropped")
            default = "unscaled"
        }
    }

    fun dcraw(
        dcraw: String,
        option: String,
        file: File
    ) {
        val processBuilder = ProcessBuilder()

        val command = mutableListOf(dcraw, "-T", "-v")

        when(option) {
            "scaled" -> command.add("-d")
            "none" -> command.add("-d")
            "unscaled" -> command.add("-D")
            "none-unscaled" -> command.add("-D")
            "uncropped" -> command.add("-E")
            "none-uncropped" -> command.add("-E")
            else -> throw IllegalArgumentException("Unknown option: $option")
        }

        command.add("-4")
        command.add("-j")
        command.add("-t")
        command.add("0")
        command.add("-W")

        command.add("-O")
        command.add(file.prefixName("${name}_").replaceExtension("tif").path)

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
        val option: String by arguments

        for (inputFile in inputFiles) {
            println("Converting $inputFile")
            dcraw(dcraw, option, inputFile)
            println()
        }

        null
    }
}
