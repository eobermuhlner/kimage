package ch.obermuhlner.kimage.io

import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.File
import javax.imageio.ImageIO

object ImageReader {

    fun read(file: File): Image {
        val image = ImageIO.read(file) ?: throw RuntimeException("Failed to read image: $file")

        val color = DoubleArray(3)
        val matrixImage = MatrixImage(image.width, image.height)
        if (image.type == BufferedImage.TYPE_CUSTOM) {
            // TODO check colorModel and convert non-RGB correctly
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    image.raster.getPixel(x, y, color)

                    when (image.raster.transferType) {
                        DataBuffer.TYPE_USHORT -> for (i in color.indices) {
                            color[i] = color[i]/UShort.MAX_VALUE.toDouble()
                        }
                        DataBuffer.TYPE_SHORT -> for (i in color.indices) {
                            color[i] = color[i]/Short.MAX_VALUE.toDouble()
                        }
                        DataBuffer.TYPE_INT -> for (i in color.indices) {
                            color[i] = color[i]/Int.MAX_VALUE.toDouble()
                        }
                        DataBuffer.TYPE_BYTE -> for (i in color.indices) {
                            color[i] = color[i]/Byte.MAX_VALUE.toDouble()
                        }
                    }

                    matrixImage.setPixel(x, y, color)
                }
            }
        } else {
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val argb = image.getRGB(x, y)
                    //alpha = (argb shr 24 and 0xff) / 255.0
                    color[0] = (argb shr 16 and 0xff) / 255.0
                    color[1] = (argb shr 8 and 0xff) / 255.0
                    color[2] = (argb and 0xff) / 255.0
                    matrixImage.setPixel(x, y, color)
                }
            }
        }

        return matrixImage
    }

}