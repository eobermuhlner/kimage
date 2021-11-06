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
    name = "exiftool"
    title = "Executes the exiftool on the images"
    description = """
                Executes the exiftool on the images.
                """
    arguments {
        string("exiftool") {
            description = """
               The `exiftool` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
            default = "exiftool"
        }
    }

    multi {
        val exiftool: String by arguments

        val scriptName = this@kimage.name

        val command = mutableListOf(exiftool)
        command += "-v"
        command += "-w"
        command += "txt"
        for (inputFile in inputFiles) {
            command += inputFile.path
        }

        // TODO add script name
        //command.add(inputFile.prefixName(outputDirectory, "${scriptName}_%04d_").replaceExtension(extension).path)

        println("Command: $command")

        val processBuilder = ProcessBuilder()
        processBuilder.command(command)
        val process = processBuilder.start()

        Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
        val exitCode = process.waitFor()
        println("Exit code: $exitCode")

        null
    }
}
