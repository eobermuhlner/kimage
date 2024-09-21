import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "crop-scale"
    title = "Crop and scale image"
    description = """
                Crop and scale image to specified target size.
                """
    arguments {
        int("width") {
            description = """
                The target width.
                """
        }
        int("height") {
            description = """
                The target height.
                """
        }
        boolean("rotate") {
            description = """
                Automatically rotate to keep ratio similar. 
                """
            default = false
        }
        boolean("crop") {
            description = """
                Automatically crop. 
                """
            default = false
        }
        string("verticalAlignment") {
            description = """
                Controls how to crop vertically.
                - top = keep the top part of the image
                - center = keep the center part of the image
                - bottom = keep the bottom part of the image
                """
            allowed = listOf("top", "center", "bottom")
            default = "center"
            enabledWhen = Reference("crop").isEqual("true")
        }
        string("horizontalAlignment") {
            allowed = listOf("left", "center", "right")
            default = "center"
            description = """
                Controls how to crop horizontally.
                - left = keep the left part of the image
                - center = keep the center part of the image
                - right = keep the right part of the image
                """
            enabledWhen = Reference("crop").isEqual("true")
        }
    }

    single {
        var width: Int by arguments
        var height: Int by arguments
        var rotate: Boolean by arguments
        var crop: Boolean by arguments
        var verticalAlignment: String by arguments
        var horizontalAlignment: String by arguments

        if (rotate && width > height && inputImage.width < height) {
            val tmp = width
            width = height
            height = tmp
        }

        if (crop) {
            val sourceRatio = inputImage.width.toDouble() / inputImage.height
            val correctedNewWidth = max(width, (height*sourceRatio).toInt())
            val correctedNewHeight = max(height, (width/sourceRatio).toInt())
            val scaledImage = inputImage.scaleTo(correctedNewWidth, correctedNewHeight)
            val croppedX = when(horizontalAlignment) {
                "left" -> 0
                "center" -> (correctedNewWidth - width) / 2
                "right" -> correctedNewWidth - width
                else -> throw IllegalArgumentException("Unknown horizontalAlignment: $horizontalAlignment")
            }
            val croppedY = when(verticalAlignment) {
                "top" -> 0
                "center" -> (correctedNewHeight - height) / 2
                "bottom" -> correctedNewHeight - height
                else -> throw IllegalArgumentException("Unknown verticalAlignment: $verticalAlignment")
            }
            scaledImage.crop(croppedX, croppedY, width, height)
        } else {
            inputImage.scaleToKeepRatio(width, height)
        }
    }
}
