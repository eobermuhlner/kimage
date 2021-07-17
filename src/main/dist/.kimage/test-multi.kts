import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "test-multi"
    title = "Test script to show how to process multiple images in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        boolean("center") {
            description = "Center images to fit the first image."
            default = false
        }
    }

    // 'multi' means that all input files are processed together in a single run
    multi {
        // The processed arguments are available in a Map<String, Any> 'arguments'
        val center: Boolean by arguments // Use the kotlin delegate by feature to map the arguments into typed variables

        // Variables 'verboseMode' and 'debugMode' are automatically available
        if (verboseMode) {
            println("arguments  = $arguments")

            // The raw unprocessed arguments (no default values filled) are available in the rare case you need them
            println("rawArguments  = $rawArguments")
        }

        // The input files can now be processed
        println("inputFiles  = $inputFiles")

        // Note: In 'multi' mode there are no 'inputFile' or 'inputImage' variables
        //       You need to load the images yourself and process them.
        //       Preloading all the input images would be more convenient but might lead to out-of-memory problems

        // The following processing code is a documented version of the 'stack-average' script:

        var stacked: Image? = null // The result image is initally null

        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")
            val image = ImageReader.read(inputFile) // Load an input image (one by one - only the stacked image is kept in memory)
            stacked = if (stacked == null) {
                image // If the input image is the first image assign it to 'stacked'
            } else {
                // Ensure input image has the same size as 'stacked'
                val croppedImage = if (center) {
                    // Use the 'center' variable being true to decide to center the images first
                    val halfWidth = stacked.width/2
                    val halfHeight = stacked.height/2
                    image.crop(image.width/2 - halfWidth, image.height/2 - halfHeight, stacked.width, stacked.height)
                } else {
                    // Use the 'center' variable being false to decide to stack the images top/left
                    image.crop(0, 0, stacked.width, stacked.height)
                }
                stacked + croppedImage // Calculate pixel-wise sum of 'stacked' + 'croppedImage' and assigned it to 'stacked'
            }
        }

        // The last value in the script is the output
        if (stacked == null) {
            null // If no image was processed return null
        } else {
            stacked / inputFiles.size.toDouble() // Pixel-wise divide the stacked image by the number of input files -> average pixels
        }
    }
}
