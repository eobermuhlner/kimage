package ch.obermuhlner.kimage.io

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.image.awt.AwtBufferedImage
import ch.obermuhlner.kimage.image.tiff.TiffImage
import mil.nga.tiff.TiffReader
import java.io.File
import javax.imageio.ImageIO

object ImageReader {

    fun read(file: File): Image {
        if (file.extension.toLowerCase() in listOf("tif", "tiff")) {
            try {
                val tiffImage = TiffReader.readTiff(file)
                return TiffImage(tiffImage, false)
            } catch (ex: Exception) {
                ex.printStackTrace()
                // ignore
            }
        }

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