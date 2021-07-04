import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "calibrate"
    description = """
                Calibrates bias/dark/flat/darkflat/light images.
                """
    arguments {
    }

    multi {
        println("Calibrate")
        println()

        println("Loading bias")
        val bias = ImageReader.read(File("bias.TIF"))
        println("Loading dark")
        var dark = ImageReader.read(File("dark.TIF"))
        println("Loading darkflat")
        var darkflat = ImageReader.read(File("darkflat.TIF"))
        println("Loading flat")
        var flat = ImageReader.read(File("flat.TIF"))

        dark = dark - bias
        darkflat = darkflat - bias
        flat = flat - bias - darkflat

        for (inputFile in inputFiles) {
            println("Loading $inputFile")
            var light = ImageReader.read(inputFile)

            val light2 = light - bias - dark
            //ImageWriter.write(deltaChannel(light2, light, factor = 20.0), inputFile.prefixName("delta_light2_"))

            val outputFile = inputFile.prefixName("calibrated_")
            println("Writing $outputFile")
            val light3 = light2.pixelWiseDiv(flat)
            ImageWriter.write(light3, outputFile)
            //ImageWriter.write(deltaChannel(light3, light2), inputFile.prefixName("delta_light3_"))
        }

        null
    }
}
