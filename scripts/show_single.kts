import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.javafx.*
import ch.obermuhlner.kotlin.javafx.*
import ch.obermuhlner.kimage.javafx.KImageApplication.Companion.interactive
import java.io.*
import kotlin.math.*

require(inputSingleMode)

val file = inputFile as File
val image = inputImage as Image
val parameters = inputParameters as Map<String, String>

interactive {
    setCurrentImage(image, file.name)

    filter ("Next") {
        image
    }
}

null
