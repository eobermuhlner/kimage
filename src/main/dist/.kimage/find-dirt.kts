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
    name = "find-dirt"
    title = "Find dirt on the sensor in a flat image"
    description = """
                Find dirt on the sensor in a flat image by creating a false color image showing the distance to the median.
                """
    arguments {
        string("channel") {
            description = """
                        """
            allowed = listOf("gray", "luminance", "red", "green", "blue")
            default = "gray"
        }
        double("factor") {
            default = 2.0
        }
    }

    single {
        val channel: String by arguments
        val factor: Double by arguments

        val measureChannel = when (channel) {
            "gray" -> Channel.Gray
            "luminance" -> Channel.Luminance
            "red" -> Channel.Red
            "green" -> Channel.Green
            "blue" -> Channel.Blue
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }

        val measureMatrix = inputImage[measureChannel]

        val median = measureMatrix.median();

        val f = 0.2
        var delta = Matrix.matrixOf(measureMatrix.width, measureMatrix.height) { x, y ->
            (measureMatrix[x, y] - median)
        }
        val min = delta.min()
        val max = delta.max()
        delta.onEach { v ->
            (v + min)/(max - min) - min / (max - min)
        }
        delta.onEach { v ->
            if (v >= 0.0) {
                v.pow(1.0 / factor)
            } else {
                -((-v).pow(1.0 / factor))
            }
        }

        val red = delta.copy().onEach   { v -> if (v < 0.0) -v     else v * f }
        val green = delta.copy().onEach { v -> if (v < 0.0) -v * f else v * f }
        val blue = delta.copy().onEach  { v -> if (v < 0.0) -v * f else v     }


        MatrixImage(measureMatrix.width, measureMatrix.height,
            Channel.Red to red,
            Channel.Green to green,
            Channel.Blue to blue
        )
    }
}
