package ch.obermuhlner.kimage.io

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.awt.AwtBufferedImage
import ch.obermuhlner.kimage.image.tiff.TiffImage
import mil.nga.tiff.TiffWriter
import java.io.File
import javax.imageio.ImageIO

object ImageWriter {
    fun write(image: Image, output: File) {
        val name = output.name
        for (format in ImageFormat.values()) {
            for (extension in format.extensions) {
                if (name.length > extension.length && name.substring(name.length - extension.length).equals(extension, ignoreCase = true)) {
                    write(image, output, format)
                    return
                }
            }
        }
        write(image, output, ImageFormat.TIF)
    }

    fun write(image: Image, output: File, format: ImageFormat) {
        when  {
            image is TiffImage && image.writable -> {
                TiffWriter.writeTiff(output, image.tiffImage)
            }
            image is AwtBufferedImage -> {
                ImageIO.write(image.bufferedImage, format.name, output)
            }
            else -> {
                if (format == ImageFormat.TIF) {
                    write(TiffImage.from(image), output, format)
                } else {
                    write(AwtBufferedImage.from(image), output, format)
                }
            }
        }
    }
}