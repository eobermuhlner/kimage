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
    name = "convert-frames"
    title = "Convert a video frames into images"
    description = """
                Convert an a video frames into images.
                """
    arguments {
        string("ffmpeg") {
            description = """
               The `ffmpeg` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
            default = "ffmpeg"
        }
        string("extension") {
            default = "tif"
        }
    }

    multi {
        val ffmpeg: String by arguments
        val extension: String by arguments

        val scriptName = this@kimage.name

        for (inputFile in inputFiles) {
            println("Converting $inputFile")

            val processBuilder = ProcessBuilder()

            val command = mutableListOf(ffmpeg, "-i", inputFile.path)

            // TODO add script name
            command.add(inputFile.prefixName(outputDirectory, "${scriptName}_%04d_").replaceExtension(extension).path)

            println("Command: $command")

            processBuilder.command(command)

            val process = processBuilder.start()

            Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
            val exitCode = process.waitFor()
            println("Exit code: $exitCode")
        }

        null
    }
}
