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
    name = "platesolve-astap"
    title = "Platesolve an astrophotography image"
    description = """
                Platesolves an astrophotography image.
                """
    arguments {
        string("astap") {
            description = """
               The `astap` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
            default = "C:\\Program Files\\astap\\astap_cli"
        }
    }

    multi {
        val astap: String by arguments

        val scriptName = this@kimage.name

        for (inputFile in inputFiles) {
            println("Converting $inputFile")

            val processBuilder = ProcessBuilder()

            val command = mutableListOf(astap, "-f", inputFile.path)
            println("Command: $command")
            processBuilder.command(command)

            val process = processBuilder.start()

            Executors.newSingleThreadExecutor().submit(StreamGobbler(process.inputStream, System.out::println))
            Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
            val exitCode = process.waitFor()
            println("Exit code: $exitCode")
        }

        null
    }
}
