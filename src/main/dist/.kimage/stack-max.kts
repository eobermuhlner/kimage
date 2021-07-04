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
                Stacks multiple images by calculating a pixel-wise maximum.
                
                This stacking script is useful to find outliers and badly aligned images.
                """
    arguments {
    }

    multi {
        println("Stack multiple images using max")

        var stacked: Image? = null
        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")
            val image = ImageReader.read(inputFile)
            stacked = if (stacked == null) {
                image
            } else {
                max(stacked, image)
            }
        }

        stacked
    }
}
