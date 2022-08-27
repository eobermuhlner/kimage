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
    name = "value-mask"
    title = "Create a value range mask."
    description = """
                Create a mask image based on a value range.
                """
    arguments {
        double("low") {
            min = 0.0
            max = 1.0
            default = 0.0
        }
        double("high") {
            min = 0.0
            max = 1.0
            default = 1.0
        }
        int("blur") {
            min = 0
            default = 0
        }
        boolean("invert") {
            default = false
        }
    }

    single {
        val low: Double by arguments
        val high: Double by arguments
        val blur: Int by arguments
        val invert: Boolean by arguments

        var matrix = inputImage[Channel.Gray]
        matrix.onEach { _, _, value ->
            if (value < low) {
                0.0
            } else if (value > high) {
                0.0
            } else {
                value
            }
        }

        if (blur > 0) {
            matrix = matrix.gaussianBlurFilter(blur)
        }
        if (invert) {
            matrix.onEach { _, _, value ->
                1.0 - value
            }
        }

        MatrixImage(matrix)
    }
}
