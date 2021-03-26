package ch.obermuhlner.kimage.image.tiff

import ch.obermuhlner.kimage.image.AbstractImage
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.util.TiffConstants

class TiffImage(val tiffImage: TIFFImage, val writable: Boolean = false, channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue))
    : AbstractImage(tiffImage.fileDirectories[0].imageWidth.toInt(), tiffImage.fileDirectories[0].imageHeight.toInt(), channels) {

    private val rasters = if (writable) tiffImage.fileDirectories[0].writeRasters else tiffImage.fileDirectories[0].readRasters()

    override fun getPixel(x: Int, y: Int, channel: Channel): Double {
        val value = rasters.getPixelSample(channelIndex(channel), x, y)
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            else -> value.toDouble() / 256.0
        }
    }

    override fun setPixel(x: Int, y: Int, channel: Channel, color: Double) {
        rasters.setPixelSample(channelIndex(channel), x, y, color)
    }

    companion object {
        fun from(image: Image): TiffImage {
            val tiffImage = create(image.width, image.height)
            tiffImage.setPixels(image)
            return tiffImage
        }

        fun create(width: Int, height: Int): TiffImage {
            val samplesPerPixel = 3
            val fieldType = FieldType.FLOAT
            val bitsPerSample = fieldType.bits
            val rasters = Rasters(width, height, samplesPerPixel, fieldType)
            val rowsPerStrip = rasters.calculateRowsPerStrip(TiffConstants.PLANAR_CONFIGURATION_CHUNKY)
            val directory = FileDirectory()
            directory.setImageWidth(width)
            directory.setImageHeight(height)
            directory.bitsPerSample = listOf(bitsPerSample, bitsPerSample, bitsPerSample)
            directory.compression = TiffConstants.COMPRESSION_NO
            directory.photometricInterpretation = TiffConstants.PHOTOMETRIC_INTERPRETATION_RGB
            directory.samplesPerPixel = samplesPerPixel
            directory.setRowsPerStrip(rowsPerStrip)
            directory.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
            directory.setSampleFormat(TiffConstants.SAMPLE_FORMAT_FLOAT)
            directory.writeRasters = rasters

            val tiffImage = TIFFImage()
            tiffImage.add(directory)

            return TiffImage(tiffImage, true)
        }
    }
}