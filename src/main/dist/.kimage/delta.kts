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
    title = "Create delta images between the first image and all other images"
    description = """
                The output images show the pixel-wise difference between two images on a specific channel (default is `Luminance`).
                
                The difference is color coded:
                  - black = no difference
                  - blue  = pixel in the first image is brighter
                  - red   = pixel in the first image is darker
                  
                The `factor` argument controls how much the differences are exaggerated.
                 
                This script is a useful to compare images, especially outputs of other scripts with different arguments.
                """
    arguments {
        double("factor") {
            description = """
                Controls how much the differences are exaggerated.
            """
            default = 5.0
        }
        string("channel") {
            description = """
                The channel used to calculate the difference between two images.                
            """
            allowed = listOf("Red", "Green", "Blue", "Luminance", "Gray")
            default = "Luminance"
        }
    }

    multi {
        val factor: Double by arguments
        val channel: String by arguments

        println("Loading base image ${inputFiles[0]}")
        val baseImage = ImageReader.read(inputFiles[0])
        println()

        for (i in 1 until inputFiles.size) {
            val inputFile = inputFiles[i]
            println("Loading image ${inputFile}")
            val image = ImageReader.read(inputFile)

            val deltaFile = inputFile.prefixName("delta_")
            println("Saving $deltaFile")
            val deltaImage = deltaChannel(baseImage, image, factor = factor, channel = Channel.valueOf(channel))
            ImageWriter.write(deltaImage, deltaFile)
            println()
        }
    }
}
