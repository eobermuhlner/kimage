package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import kotlin.math.abs

fun deltaRGB(image1: Image, image2: Image, factor: Double = 10.0): Image {
    return MatrixImage(
        image1.width,
        image2.height,
        image1.channels) { channel, _, _ ->
        (image1[channel] - image2[channel]) * factor
    }
}

fun deltaChannel(image1: Image, image2: Image, factor: Double = 10.0, channel: Channel = Channel.Luminance): Image {
    val delta = image1[channel] - image2[channel]
    val red = delta.point { v -> if (v < 0.0) -v * factor else v * factor * 0.5 }
    val green = delta.point { v -> abs(v) * factor * 0.5 }
    val blue = delta.point { v -> if (v > 0.0) v * factor else -v * factor * 0.5 }
    return MatrixImage(
        image1.width,
        image2.height,
        Channel.Red to red,
        Channel.Green to green,
        Channel.Blue to blue)
}

operator fun Image.minus(other: Image): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel] - other[channel]
    }
}

operator fun Image.times(value: Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel] * value
    }
}

