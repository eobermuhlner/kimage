import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "stack-average"
    description = """
                Test script to show how to handle multiple images in a kimage script.
                """
    arguments {
    }

    multi {
        println("Stack multiple images using average")

        var sumImage: Image = ImageReader.readMatrixImage(inputFiles[0])

        for (index in 1 until inputFiles.size) {
            val inputFile = inputFiles[index]
            println("Loading image: $inputFile")
            val image = ImageReader.readMatrixImage(inputFile).crop(0, 0, sumImage.width, sumImage.height)
            sumImage += image
        }

        sumImage / inputFiles.size.toDouble()
    }
}
