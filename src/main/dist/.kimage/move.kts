import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "move"
    title = "Move an image"
    description = """
                Move an image by an offset along the x and y axis.
                """
    arguments {
        double("x") {
            description = """
                The delta along the x axis to move the image.
                """
            hint = Hint.ImageDeltaX
        }
        double("y") {
            description = """
                The delta along the y axis to move the image.
                """
            hint = Hint.ImageDeltaY
        }
    }

    single {
        var x: Double by arguments
        var y: Double by arguments

        inputImage.scaleBy(1.0, 1.0, x, y)
    }
}
