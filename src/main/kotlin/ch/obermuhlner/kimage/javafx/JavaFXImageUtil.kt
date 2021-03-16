package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min

object JavaFXImageUtil {

    fun toWritableImage(image: Image): WritableImage {
        val color = DoubleArray(3)
        val output = WritableImage(image.width, image.height)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.getPixel(x, y, color)
                color[0] = max(0.0, min(color[0], 1.0))
                color[1] = max(0.0, min(color[1], 1.0))
                color[2] = max(0.0, min(color[2], 1.0))
                output.pixelWriter.setColor(x, y, Color(color[0], color[1], color[2], 1.0))
            }
        }
        return output
    }
}