package ch.obermuhlner.kimage.io

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.image.awt.AwtBufferedImage
import java.io.File
import javax.imageio.ImageIO

object ImageReader {

    fun read(file: File): Image {
        val image = ImageIO.read(file) ?: throw RuntimeException("Failed to read image: $file")
        return AwtBufferedImage(image)
    }

    fun readMatrixImage(file: File): MatrixImage {
        val image = read(file)
        val matrixImage = MatrixImage(image.width, image.height)
        matrixImage.setPixels(image)
        return matrixImage
    }
}