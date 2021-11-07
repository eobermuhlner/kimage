package ch.obermuhlner.kimage.image

import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.floor

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
            Channel.Hue -> convertRGBtoHSB(getPixel(x, y, Channel.Red), getPixel(x, y, Channel.Green), getPixel(x, y, Channel.Blue))[0]
            Channel.Saturation -> convertRGBtoHSB(getPixel(x, y, Channel.Red), getPixel(x, y, Channel.Green), getPixel(x, y, Channel.Blue))[1]
            Channel.Brightness -> convertRGBtoHSB(getPixel(x, y, Channel.Red), getPixel(x, y, Channel.Green), getPixel(x, y, Channel.Blue))[2]
            Channel.Red -> convertHSBtoRGB(getPixel(x, y, Channel.Hue), getPixel(x, y, Channel.Saturation), getPixel(x, y, Channel.Brightness))[0]
            Channel.Green -> convertHSBtoRGB(getPixel(x, y, Channel.Hue), getPixel(x, y, Channel.Saturation), getPixel(x, y, Channel.Brightness))[1]
            Channel.Blue -> convertHSBtoRGB(getPixel(x, y, Channel.Hue), getPixel(x, y, Channel.Saturation), getPixel(x, y, Channel.Brightness))[2]
            else -> 0.0
        }
    }

    fun convertRGBtoHSB(r: Double, g: Double, b: Double, hsbvals: DoubleArray = DoubleArray(3)): DoubleArray {
        var hue: Double
        val saturation: Double
        val brightness: Double
        var cmax = if (r > g) r else g
        if (b > cmax) cmax = b
        var cmin = if (r < g) r else g
        if (b < cmin) cmin = b
        brightness = cmax
        saturation = if (cmax != 0.0) (cmax - cmin) / cmax else 0.0
        if (saturation == 0.0) {
            hue = 0.0
        } else {
            val redc = (cmax - r) / (cmax - cmin)
            val greenc = (cmax - g) / (cmax - cmin)
            val bluec = (cmax - b) / (cmax - cmin)
            hue = if (r == cmax) bluec - greenc else if (g == cmax) 2.0 + redc - bluec else 4.0 + greenc - redc
            hue = hue / 6.0
            if (hue < 0) hue = hue + 1.0
        }
        hsbvals[0] = hue // * 360
        hsbvals[1] = saturation
        hsbvals[2] = brightness
        return hsbvals
    }

    fun convertHSBtoRGB(hue: Double, saturation: Double, brightness: Double, rgbvals: DoubleArray = DoubleArray(3)): DoubleArray {
        var r = 0.0
        var g = 0.0
        var b = 0.0
        if (saturation == 0.0) {
            b = brightness
            g = b
            r = g
        } else {
            val h = (hue - floor(hue)) * 6.0
            val f = h - floor(h)
            val p = brightness * (1.0 - saturation)
            val q = brightness * (1.0 - saturation * f)
            val t = brightness * (1.0 - saturation * (1.0 - f))
            when (h.toInt()) {
                0 -> {
                    r = brightness
                    g = t
                    b = p
                }
                1 -> {
                    r = q
                    g = brightness
                    b = p
                }
                2 -> {
                    r = p
                    g = brightness
                    b = t
                }
                3 -> {
                    r = p
                    g = q
                    b = brightness
                }
                4 -> {
                    r = t
                    g = p
                    b = brightness
                }
                5 -> {
                    r = brightness
                    g = p
                    b = q
                }
            }
        }

        rgbvals[0] = r
        rgbvals[1] = g
        rgbvals[2] = b

        return rgbvals
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
        crop(targetX, targetY, width, height).setPixels(source.crop(sourceX, sourceY, width, height), outsideColor)
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
        val m = DoubleMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                m[x, y] = getPixel(x, y, channel)
            }
        }
        return m
    }

    fun isInside(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x < width && y < height
    }

    fun boundedX(x: Int) = clamp(x, 0, width - 1)
    fun boundedY(y: Int) = clamp(y, 0, height - 1)
}