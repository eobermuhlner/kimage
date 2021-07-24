package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.math.clamp
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.min

object JavaFXImageUtil {

    fun toWritableImage(image: Image): WritableImage {
        val color = DoubleArray(3)
        val output = WritableImage(image.width, image.height)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.getPixel(x, y, color)
                color[0] = clamp(color[0], 0.0, 1.0)
                color[1] = clamp(color[1], 0.0, 1.0)
                color[2] = clamp(color[2], 0.0, 1.0)
                output.pixelWriter.setColor(x, y, Color(color[0], color[1], color[2], 1.0))
            }
        }
        return output
    }

    fun copy(source: Image, target: WritableImage) {
        val color = DoubleArray(3)
        val width = min(source.width, target.width.toInt())
        val height = min(source.height, target.height.toInt())
        for (y in 0 until height) {
            for (x in 0 until width) {
                source.getPixel(x, y, color)
                color[0] = clamp(color[0], 0.0, 1.0)
                color[1] = clamp(color[1], 0.0, 1.0)
                color[2] = clamp(color[2], 0.0, 1.0)
                target.pixelWriter.setColor(x, y, Color(color[0], color[1], color[2], 1.0))
            }
        }
    }

    fun copy(source: WritableImage, target: WritableImage) {
        val width = min(source.width, target.width).toInt()
        val height = min(source.height, target.height).toInt()
        for (y in 0 until height) {
            for (x in 0 until width) {
                target.pixelWriter.setArgb(x, y, source.pixelReader.getArgb(x, y))
            }
        }
    }
}