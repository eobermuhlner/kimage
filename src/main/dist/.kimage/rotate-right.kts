import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "rotate-right"
    title = "Rotate image 90 degrees right"
    description = """
                Rotate image 90 degrees right.
                """
    arguments {
    }

    single {
        inputImage.rotateRight()
    }
}
