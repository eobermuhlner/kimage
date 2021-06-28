import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "stack-max"
    description = """
                Stacks multiple image by calculating a pixel-wise maximum.
                
                This stacking script is useful to find outliers and badly aligned images.
                """
    arguments {
    }

    multi {
        println("Stack multiple images using max")

        var stackedImage: Image = ImageReader.readMatrixImage(inputFiles[0])

        for (index in 1 until inputFiles.size) {
            val inputFile = inputFiles[index]
            println("Loading image: $inputFile")
            val image = ImageReader.readMatrixImage(inputFile).crop(0, 0, stackedImage.width, stackedImage.height)
            stackedImage = max(stackedImage, image)
        }

        stackedImage
    }
}
