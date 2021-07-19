import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "rotate-left"
    title = "Rotate image 90 degrees left."
    description = """
                Rotate image 90 degrees left.
                """
    arguments {
    }

    single {
        inputImage.rotateLeft()
    }
}
