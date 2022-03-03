package ch.obermuhlner.kimage.io

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.matrix.FloatMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import nom.tam.fits.BasicHDU
import nom.tam.fits.Fits
import nom.tam.fits.ImageHDU
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.io.File
import javax.imageio.ImageIO

object ImageReader {

    fun read(file: File): Image {
        if (file.extension == "fits" || file.extension == "fit") {
            return readFits(file)
        }

        val image = try {
            ImageIO.read(file)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to read image: $file", ex)
        } ?: throw RuntimeException("Failed to read image: $file")

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

    private fun readFits(file: File): Image {
        val fits = Fits(file)
        val hdu = fits.getHDU(0)
        if (hdu is ImageHDU) {
            val matrices = mutableListOf<Matrix>()
            var width: Int = 0
            var height: Int = 0
            when (hdu.axes.size) {
                2 -> {
                    height = hdu.axes[0]
                    width = hdu.axes[1]

                    val matrix = FloatMatrix(width, height)

                    matrices += when (hdu.bitPix) {
                        BasicHDU.BITPIX_BYTE -> {
                            val data = hdu.kernel as Array<ByteArray>
                            matrix.onEach { x, y, _ ->
                                scaleFitsValue(data[y][x].toDouble(), hdu)
                            }
                        }
                        BasicHDU.BITPIX_SHORT -> {
                            val data = hdu.kernel as Array<ShortArray>
                            matrix.onEach { x, y, _ ->
                                scaleFitsValue(data[y][x].toDouble(), hdu)
                            }
                        }
                        BasicHDU.BITPIX_INT -> {
                            val data = hdu.kernel as Array<IntArray>
                            matrix.onEach { x, y, _ ->
                                scaleFitsValue(data[y][x].toDouble(), hdu)
                            }
                        }
                        BasicHDU.BITPIX_LONG -> {
                            val data = hdu.kernel as Array<LongArray>
                            matrix.onEach { x, y, _ ->
                                scaleFitsValue(data[y][x].toDouble(), hdu)
                            }
                        }
                        BasicHDU.BITPIX_FLOAT -> {
                            val data = hdu.kernel as Array<FloatArray>
                            matrix.onEach { x, y, _ ->
                                scaleFitsValue(data[y][x].toDouble(), hdu)
                            }
                        }
                        BasicHDU.BITPIX_DOUBLE -> {
                            val data = hdu.kernel as Array<DoubleArray>
                            matrix.onEach { x, y, _ ->
                                scaleFitsValue(data[y][x], hdu)
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown bits per pixel: ${hdu.bitPix}")
                    }
                }
                3 -> {
                    val channels = hdu.axes[0]
                    height = hdu.axes[1]
                    width = hdu.axes[2]

                    for (channelIndex in 0 until channels) {
                        val matrix = FloatMatrix(width, height)

                        matrices += when (hdu.bitPix) {
                            BasicHDU.BITPIX_BYTE -> {
                                val data = hdu.kernel as Array<Array<ByteArray>>
                                matrix.onEach { x, y, _ ->
                                    scaleFitsValue(data[channelIndex][y][x].toDouble(), hdu)
                                }
                            }
                            BasicHDU.BITPIX_SHORT -> {
                                val data = hdu.kernel as Array<Array<ShortArray>>
                                matrix.onEach { x, y, _ ->
                                    scaleFitsValue(data[channelIndex][y][x].toDouble(), hdu)
                                }
                            }
                            BasicHDU.BITPIX_INT -> {
                                val data = hdu.kernel as Array<Array<IntArray>>
                                matrix.onEach { x, y, _ ->
                                    scaleFitsValue(data[channelIndex][y][x].toDouble(), hdu)
                                }
                            }
                            BasicHDU.BITPIX_LONG -> {
                                val data = hdu.kernel as Array<Array<LongArray>>
                                matrix.onEach { x, y, _ ->
                                    scaleFitsValue(data[channelIndex][y][x].toDouble(), hdu)
                                }
                            }
                            BasicHDU.BITPIX_FLOAT -> {
                                val data = hdu.kernel as Array<Array<FloatArray>>
                                matrix.onEach { x, y, _ ->
                                    scaleFitsValue(data[channelIndex][y][x].toDouble(), hdu)
                                }
                            }
                            BasicHDU.BITPIX_DOUBLE -> {
                                val data = hdu.kernel as Array<Array<DoubleArray>>
                                matrix.onEach { x, y, _ ->
                                    scaleFitsValue(data[channelIndex][y][x], hdu)
                                }
                            }
                            else -> throw IllegalArgumentException("Unknown bits per pixel: ${hdu.bitPix}")
                        }
                    }
                }
            }

            return when (matrices.size) {
                1 -> MatrixImage(width, height,
                    Channel.Gray to matrices[0])
                3 -> MatrixImage(width, height,
                    Channel.Red to matrices[0],
                    Channel.Green to matrices[1],
                    Channel.Blue to matrices[2])
                4 -> MatrixImage(width, height,
                    Channel.Red to matrices[0],
                    Channel.Green to matrices[1],
                    Channel.Blue to matrices[2],
                    Channel.Alpha to matrices[3])
                else -> throw java.lang.IllegalArgumentException("Unknown number of channels in fits: ${matrices.size}")
            }
        }

        throw IllegalArgumentException("Unknown FITS")
    }

    private fun scaleFitsValue(value: Double, hdu: BasicHDU<*>): Double {
        return if (hdu.minimumValue != hdu.maximumValue) {
            hdu.bZero + (value - hdu.minimumValue) / (hdu.maximumValue - hdu.minimumValue) * hdu.bScale
        } else {
            val scaledValue = hdu.bZero + value * hdu.bScale
            when (hdu.bitPix) {
                BasicHDU.BITPIX_BYTE -> scaledValue / (256.0 - 1)
                BasicHDU.BITPIX_SHORT -> scaledValue / (65536.0 - 1)
                BasicHDU.BITPIX_INT -> scaledValue / (4294967296.0 - 1)
                BasicHDU.BITPIX_LONG -> scaledValue / (18446744073709551616.0 - 1)
                BasicHDU.BITPIX_FLOAT -> scaledValue
                else -> throw RuntimeException("Unknown bitpix: " + hdu.bitPix)
            }
        }
    }

}