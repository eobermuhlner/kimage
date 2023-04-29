import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "stack-max"
    title = "Stack multiple images by calculating a pixel-wise maximum"
    description = """
                This stacking script is useful to find outliers and badly aligned images.
                This implementation is faster and uses less memory than using the generic script `stack --arg method=max`.
                """
    arguments {
    }

    multi {
        var stacked: Image? = null
        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")
            val image = ImageReader.read(inputFile)
            stacked = if (stacked == null) {
                image
            } else {
                max(stacked, image.crop(0, 0, stacked.width, stacked.height))
            }
        }

        stacked
    }
}
