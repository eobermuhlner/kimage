import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "spooky"
    description = """
                Spooky cool effect.
                """

    single {
        println("Spooky cool effect")

        val background = inputImage.medianFilter(10).gaussianBlurFilter(10)
        inputImage - background
    }
}
