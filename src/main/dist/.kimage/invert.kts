import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "invert"
    title = "Invert an image"
    description = """
                Invert an image.
                """
    arguments {
    }

    single {
        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ ->
            inputImage[channel].onEach { value -> 1.0 - value }
        }
    }
}
