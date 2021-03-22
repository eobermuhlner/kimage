package ch.obermuhlner.kimage.image

import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix

interface Image {
    val width: Int
    val height: Int
    val channels: List<Channel>

    operator fun get(x: Int, y: Int): DoubleArray = getPixel(x, y)
    operator fun set(x: Int, y: Int, color: DoubleArray) = setPixel(x, y, color)

    operator fun get(x: Int, y: Int, channel: Channel): Double = getPixel(x, y, channel)
    operator fun set(x: Int, y: Int, channel: Channel, color: Double) = setPixel(x, y, channel, color)

    fun getPixel(x: Int, y: Int, color: DoubleArray = DoubleArray(channels.size)): DoubleArray {
        for (i in channels.indices) {
            color[i] = getPixel(x, y, channels[i])
        }
        return color
    }

    fun setPixel(x: Int, y: Int, color: DoubleArray) {
        for (i in channels.indices) {
            setPixel(x, y, channels[i], color[i])
        }
    }

    fun getPixel(x: Int, y: Int, channel: Channel): Double
    fun setPixel(x: Int, y: Int, channel: Channel, color: Double)

    fun convertPixelToChannel(x: Int, y: Int, toChannel: Channel): Double {
        return when (toChannel) {
            Channel.Luminance -> 0.2126 * getPixel(x, y, Channel.Red) + 0.7152 * getPixel(x, y, Channel.Green) +  0.0722 * getPixel(x, y, Channel.Blue)
            Channel.Gray -> (getPixel(x, y, Channel.Red) + getPixel(x, y, Channel.Green) + getPixel(x, y, Channel.Blue)) / 3.0
            Channel.Alpha -> 1.0
            else -> 0.0
        }
    }

    fun getPixel(x: Int, y: Int, targetChannels: List<Channel>, color: DoubleArray = DoubleArray(targetChannels.size)) {
        for (targetChannelIndex in targetChannels.indices) {
            val targetChannel = targetChannels[targetChannelIndex]
            val value = getPixel(x, y, targetChannel)
            color[targetChannelIndex] = value
        }
    }

    fun channelIndex(channel: Channel) = channels.indexOf(channel)

    fun hasChannel(channel: Channel) = channelIndex(channel) >= 0

    fun setPixels(fillColor: DoubleArray) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                this[x, y] = fillColor
            }
        }
    }

    fun setPixels(sourceX: Int, sourceY: Int, source: Image, targetX: Int, targetY: Int, width: Int, height: Int, outsideColor: DoubleArray? = null) {
        croppedImage(targetX, targetY, width, height).setPixels(source.croppedImage(sourceX, sourceY, width, height), outsideColor)
    }

    fun setPixels(source: Image, outsideColor: DoubleArray? = null) {
        val color = DoubleArray(channels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (outsideColor == null || source.isInside(x, y)) {
                    source.getPixel(x, y, channels, color)
                    setPixel(x, y, color)
                } else {
                    setPixel(x, y, outsideColor)
                }
            }
        }
    }

    operator fun get(channel: Channel): Matrix = getMatrix(channel)

    fun getMatrix(channel: Channel): Matrix {
        val m = DoubleMatrix(height, width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                m[y, x] = getPixel(x, y, channel)
            }
        }
        return m
    }

    fun croppedImage(croppedX: Int, croppedY: Int, croppedWidth: Int, croppedHeight: Int, strictClipping: Boolean = true): Image {
        return CroppedImage(this, croppedX, croppedY, croppedWidth, croppedHeight, strictClipping)
    }

    fun isInside(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x < width && y < height
    }

    fun boundedX(x: Int) = clamp(x, 0, width - 1)
    fun boundedY(y: Int) = clamp(y, 0, height - 1)
}