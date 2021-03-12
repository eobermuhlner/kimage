package ch.obermuhlner.kimage.image.awt

import ch.obermuhlner.kimage.image.AbstractImage
import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.matrix.Matrix
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

class AwtBufferedImage(val bufferedImage: BufferedImage, channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue)): AbstractImage(bufferedImage.width, bufferedImage.height, channels) {

    override fun getPixel(x: Int, y: Int, channel: Int): Double {
        val rgb = bufferedImage.getRGB(x, y)
        return when (channel) {
            0 -> (rgb shr 16 and 0xff) / 255.0
            1 -> (rgb shr 8 and 0xff) / 255.0
            2 -> (rgb and 0xff) / 255.0
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }
    }

    override fun setPixel(x: Int, y: Int, channel: Int, color: Double) {
        val intColor = max(min((color * 256).toInt(), 255), 0)
        val rgb = bufferedImage.getRGB(x, y)

        val newRgb = when (channel) {
            0 -> (rgb and 0x00ffff) or (intColor shl 16)
            1 -> (rgb and 0xff00ff) or (intColor shl 8)
            2 -> (rgb and 0xffff00) or intColor
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }
        bufferedImage.setRGB(x, y, newRgb)
    }

    companion object {
        fun from(image: Image): AwtBufferedImage {
            val awtBufferedImage = if (image.hasChannel(Channel.Gray)) {
                AwtBufferedImage(BufferedImage(image.width, image.height, BufferedImage.TYPE_USHORT_GRAY), listOf(Channel.Gray))
            } else {
                AwtBufferedImage(BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB))
            }

            awtBufferedImage.setPixels(image)
            return awtBufferedImage
        }
    }
}