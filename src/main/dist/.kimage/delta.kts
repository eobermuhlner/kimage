import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "delta"
    description = """
                Creates delta images between the first image and all other images.
                """
    arguments {
        double("factor") {
            default = 5.0
        }
        string("channel") {
            allowed = listOf("Red", "Green", "Blue", "Luminance", "Gray")
            default = "Luminance"
        }
    }

    multi {
        println("Delta image analysis")
        println()

        val factor: Double by arguments
        val channel: String by arguments

        println("Arguments:")
        println("  factor = $factor")
        println("  channel = $channel")
        println()

        println("Loading base image ${inputFiles[0]}")
        val baseImage = ImageReader.read(inputFiles[0])
        println()

        for (i in 1 until inputFiles.size) {
            val inputFile = inputFiles[i]
            println("Loading image ${inputFile}")
            val image = ImageReader.read(inputFile)

            val deltaFile = File("delta_" + inputFile.name)
            println("Saving $deltaFile")
            val deltaImage = deltaChannel(baseImage, image, factor = factor, channel = Channel.valueOf(channel))
            ImageWriter.write(deltaImage, deltaFile)
            println()
        }
    }
}
