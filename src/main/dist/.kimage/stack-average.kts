import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "stack-average"
    title = "Stack multiple images by calculating a pixel-wise average"
    description = """
                This stacking script is useful if there are no outliers and if the more powerful `stack` script fails for technical reasons.
                This implementation is faster and uses less memory than using the generic script `stack --arg method=average`.
                """
    arguments {
    }

    multi {
        println("Stack multiple images using average")

        var stacked: Image? = null
        for (inputFile in inputFiles) {
            println("Loading image: $inputFile")
            val image = ImageReader.read(inputFile)
            stacked = if (stacked == null) {
                image
            } else {
                stacked + image
            }
        }

        if (stacked == null)  null else stacked / inputFiles.size.toDouble()
    }
}
