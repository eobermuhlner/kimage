package ch.obermuhlner.kimage.javafx

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import javafx.scene.image.WritableImage
import kotlin.math.max
import kotlin.math.min

class JavafxWritableImage(
    private val image: WritableImage,
    override val channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue))
    : Image {

    override val width: Int
        get() = image.width.toInt()

    override val height: Int
        get() = image.height.toInt()

    override fun getPixel(x: Int, y: Int, channel: Channel): Double {
        val rgb: Int = image.pixelReader.getArgb(x, y)
        return when (channel) {
            Channel.Alpha -> ((rgb shr 24) and 0xff) / 255.0
            Channel.Red -> ((rgb shr 16) and 0xff) / 255.0
            Channel.Green -> ((rgb shr 8) and 0xff) / 255.0
            Channel.Blue -> (rgb and 0xff) / 255.0
            Channel.Gray -> (rgb and 0xff) / 255.0
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }
    }

    override fun setPixel(x: Int, y: Int, color: DoubleArray) {
        var argb = 0
        for (i in channels.indices) {
            val channel = channels[i]
            val intColor = max(min((color[i] * 256).toInt(), 255), 0)
            argb = argb or when (channel) {
                Channel.Alpha -> intColor shl 24
                Channel.Red -> intColor shl 16
                Channel.Green -> intColor shl 8
                Channel.Blue -> intColor
                Channel.Gray -> (intColor shl 16) or (intColor shl 8) or intColor
                else -> throw IllegalArgumentException("Unknown channel: $channel")
            }
        }
        image.pixelWriter.setArgb(x, y, argb)
    }

    override fun setPixel(x: Int, y: Int, channel: Channel, color: Double) {
        val rgb: Int = image.pixelReader.getArgb(x, y)
        val intColor = max(min((color * 256).toInt(), 255), 0)
        val newRgb = when (channel) {
            Channel.Alpha -> (rgb and 0x00ffffff) or (intColor shl 24)
            Channel.Red -> (rgb and 0x00ffff) or (intColor shl 16)
            Channel.Green -> (rgb and 0xff00ff) or (intColor shl 8)
            Channel.Blue -> (rgb and 0xffff00) or intColor
            Channel.Gray -> (intColor shl 16) or (intColor shl 8) or intColor
            else -> throw IllegalArgumentException("Unknown channel: $channel")
        }
        image.pixelWriter.setArgb(x, y, newRgb)
    }

}