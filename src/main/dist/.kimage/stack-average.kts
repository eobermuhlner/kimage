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
                Stacks multiple images by calculating a pixel-wise average.
                
                This stacking script is useful if there are no outliers. 
                """
    arguments {
    }

    multi {
        println("Stack multiple images using average")

        val sumImage = inputFiles.map {
            println("Loading image: $it")
            ImageReader.readMatrixImage(it) as Image
        } .reduce { sum, img ->
            sum + img.crop(0, 0, stacked.width, stacked.height)
        }

        sumImage / inputFiles.size.toDouble()
    }
}
