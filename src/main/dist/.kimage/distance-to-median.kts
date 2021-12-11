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
    name = "distance-to-median"
    title = "Visualize the distance to median for every pixel."
    description = """
                Creates a false color image showing the distance to the median.
                
                The difference is color coded:
                  - black = no difference to median 
                  - blue  = pixel has a value that is higher than the median value of the entire image
                  - red   = pixel has a value that is lower than the median value of the entire image

                The `factor` argument controls how much the differences are exaggerated.

                Useful to find dirt on the sensor in a flat image.
                Useful to visualize light pollution and vignetting in an image.
                """
    arguments {
        string("channel") {
            description = """
                The channel to measure the distance.
                """
            allowed = listOf("gray", "luminance", "red", "green", "blue")
            default = "gray"
        }
        double("factor") {
            description = """
                Controls how much the distances are exaggerated.
                
                A value of 1.0 means to not exaggerate
                """
            default = 2.0
            min = 0.0
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
