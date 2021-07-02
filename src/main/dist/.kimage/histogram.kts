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
    description = """
                Creates a histogram image.
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
        println("Create a histogram of an image.")
        println()

        val width: Int by arguments
        val height: Int by arguments

        val channels = listOf(Channel.Red, Channel.Green, Channel.Blue)

        println("Arguments:")
        println("  width = $width")
        println("  height = $height")
        println()

        val channelHistograms = mutableMapOf<Channel, Histogram>()
        var maxCount = 0
        for (channel in channels) {
            val histogram = Histogram(width)
            channelHistograms[channel] = histogram

            inputImage[channel].forEach { histogram.add(it) }
            maxCount = max(maxCount, histogram.max())
        }

        val output = MatrixImage(width, height)

        for (channel in channels) {
            val histogram = channelHistograms[channel]!!
            if (verboseMode) {
                println("Histogram $channel")
                histogram.print()
                println()
            }

            for (x in 0 until width) {
                val histY = (height.toDouble() * histogram[x] / maxCount).toInt()
                for (y in (height-histY) until height) {
                    output[channel][y, x] = 1.0
                }
            }
        }

        output
    }
}
