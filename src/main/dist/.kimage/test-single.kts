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
    name = "test-single"
    title = "Test script to show how to process single images in a kimage script"
    description = """
                Example script as starting point for developers.
                """
    arguments {
        boolean("saveOutput") {
            description = "Save input image as output image."
            default = false
        }
    }

    // 'single' means that every input image is processed one by one
    single {
        // The processed arguments are available in a Map 'arguments'
        val saveOutput: Boolean by arguments // Use the kotlin delegate by feature to map the arguments into typed variables

        // Variables 'verboseMode' and 'debugMode' are automatically available
        if (verboseMode) {
            println("arguments  = $arguments")

            // The raw unprocessed arguments (no default values filled) are available in the rare case you need them
            println("rawArguments  = $rawArguments")
        }

        // The input image is already preloaded:
        println("inputImage  = $inputImage")

        // The input file of the preloaded image:
        println("inputFile  = $inputFile")

        // If multiple input files are given they can be accessed even in 'single' mode
        println("inputFiles  = $inputFiles")

        // The last value in the script is the output
        if (saveOutput) {
            inputImage // Image return value will automatically be saved into output file
        } else {
            null // null return value will be ignored
        }
    }
}
