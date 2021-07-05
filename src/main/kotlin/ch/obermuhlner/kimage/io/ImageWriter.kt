package ch.obermuhlner.kimage.io

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.onEach
import ch.obermuhlner.kimage.math.clamp
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier

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
        image.onEach { v -> clamp(v, 0.0, 1.0) }

        val bufferedImage = when (format) {
            ImageFormat.TIF -> createBufferedImageUShort(image.width, image.height)
            ImageFormat.PNG -> createBufferedImageUShort(image.width, image.height)
            ImageFormat.JPG -> BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        }

        val color = DoubleArray(3)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                image.getPixel(x, y, color)

                if (bufferedImage.type == BufferedImage.TYPE_CUSTOM) {
                    when (bufferedImage.raster.transferType) {
                        DataBuffer.TYPE_USHORT -> for (i in color.indices) {
                            color[i] = color[i] * UShort.MAX_VALUE.toDouble()
                        }
                        DataBuffer.TYPE_SHORT -> for (i in color.indices) {
                            color[i] = color[i] * Short.MAX_VALUE.toDouble()
                        }
                        DataBuffer.TYPE_INT -> for (i in color.indices) {
                            color[i] = color[i] * Int.MAX_VALUE.toDouble()
                        }
                        DataBuffer.TYPE_BYTE -> for (i in color.indices) {
                            color[i] = color[i] * Byte.MAX_VALUE.toDouble()
                        }
                    }
                    bufferedImage.raster.setPixel(x, y, color)
                } else {
                    val r = (color[0] * 255.0).toInt() shl 16
                    val g = (color[1] * 255.0).toInt() shl 8
                    val b = (color[2] * 255.0).toInt()
                    bufferedImage.setRGB(x, y, r or g or b)
                }

            }
        }

        ImageIO.write(bufferedImage, format.name, output)
    }

    private fun createBufferedImageIntRGB(width: Int, height: Int): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    }

    private fun createBufferedImageUShort(width: Int, height: Int): BufferedImage {
        val specifier = ImageTypeSpecifier.createInterleaved(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            intArrayOf(0, 1, 2),
            DataBuffer.TYPE_USHORT,
            false,
            false
        )
        return specifier.createBufferedImage(width, height)
    }

    private fun createBufferedImageFloat(width: Int, height: Int): BufferedImage {
        val specifier = ImageTypeSpecifier.createInterleaved(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            intArrayOf(0, 1, 2),
            DataBuffer.TYPE_FLOAT,
            false,
            false
        )
        return specifier.createBufferedImage(width, height)
    }
}