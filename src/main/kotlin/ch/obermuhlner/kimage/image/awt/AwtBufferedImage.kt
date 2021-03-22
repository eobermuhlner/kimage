package ch.obermuhlner.kimage.image.awt

import ch.obermuhlner.kimage.image.AbstractImage
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.math.clamp
import java.awt.image.BufferedImage

class AwtBufferedImage(val bufferedImage: BufferedImage, channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue)): AbstractImage(bufferedImage.width, bufferedImage.height, channels) {

    override fun getPixel(x: Int, y: Int, channel: Channel): Double {
        val rgb = bufferedImage.getRGB(x, y)
        return when (channel) {
            Channel.Alpha -> (rgb shr 24 and 0xff) / 255.0
            Channel.Red -> (rgb shr 16 and 0xff) / 255.0
            Channel.Green -> (rgb shr 8 and 0xff) / 255.0
            Channel.Blue -> (rgb and 0xff) / 255.0
            Channel.Gray -> (rgb and 0xff) / 255.0
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }
    }

    override fun setPixel(x: Int, y: Int, channel: Channel, color: Double) {
        val intColor = clamp((color * 256).toInt(), 0, 255)
        val newRgb = when (channel) {
            Channel.Alpha -> (bufferedImage.getRGB(x, y) and 0x00ffffff) or (intColor shl 24)
            Channel.Red -> (bufferedImage.getRGB(x, y) and 0x00ffff) or (intColor shl 16)
            Channel.Green -> (bufferedImage.getRGB(x, y) and 0xff00ff) or (intColor shl 8)
            Channel.Blue -> (bufferedImage.getRGB(x, y) and 0xffff00) or intColor
            Channel.Gray -> (intColor shl 16) or (intColor shl 8) or intColor
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }
        bufferedImage.setRGB(x, y, newRgb)
    }

    companion object {
        fun from(image: Image): AwtBufferedImage {
            val awtBufferedImage = if (image.hasChannel(Channel.Gray)) {
                AwtBufferedImage(BufferedImage(image.width, image.height, BufferedImage.TYPE_USHORT_GRAY), listOf(Channel.Gray))
            } else {
                if (image.hasChannel(Channel.Alpha)) {
                    AwtBufferedImage(BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB))
                } else {
                    AwtBufferedImage(BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB))
                }
            }

            awtBufferedImage.setPixels(image)
            return awtBufferedImage
        }
    }
}