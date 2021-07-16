import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

kimage(0.1) {
    name = "convert"
    title = "Convert an image"
    description = """
                Convert an image into another format.
                """
    arguments {
        boolean("replaceExtension") {
            description = """
                Controls whether the old extension should be replaced with the new extension.
                
                - true :  Input file `example.png` would be saved as `example.tif`.
                - false : Input file `example.png` would be saved as `example.png.tif`.
                """
            default = true
        }
        string("extension") {
            description = """
                The extension to save the converted image file. 
                """
            allowed = ImageIO.getWriterFileSuffixes().toList().sorted()
        }
    }

    single {
        var replaceExtension: Boolean by arguments
        var extension: String by arguments

        val outputFile = if (replaceExtension) {
            File(inputFile.parent, "${inputFile.nameWithoutExtension}.$extension")
        } else {
            inputFile.suffixExtension(".$extension")
        }
        println("Saving $outputFile")
        ImageWriter.write(inputImage, outputFile)

        null
    }
}
