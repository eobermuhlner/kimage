import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.matrix.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "show-min-clamped"
    title = "Show all pixels with value 0."
    description = """
                Creates a false color image showing all pixels with a value of 0.
                """
    arguments {
    }

    single {
        val channelMatrices = mutableMapOf<Channel, Matrix>()
        for (channel in inputImage.channels) {
            val matrix = inputImage[channel].copy()
            matrix.onEach { v ->
                if (v <= 0.0) {
                    1.0
                } else {
                    0.0
                }
            }
            channelMatrices[channel] = matrix
        }

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ ->
            channelMatrices[channel]!!
        }
    }
}
