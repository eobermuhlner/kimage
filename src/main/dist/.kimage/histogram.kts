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
    name = "histogram"
    title = "Create a histogram image"
    description = """
                The values of every channel (RGB) are counted and a histogram is created for each channel.
                """
    arguments {
        int("width") {
            description = """
                        The width of the histogram.                        
                        """
            default = 512
        }
        int("height") {
            description = """
                        The height of the histogram.                        
                        """
            default = 300
        }
    }

    single {
        val width: Int by arguments
        val height: Int by arguments

        inputImage.histogramImage(width, height)
    }
}
