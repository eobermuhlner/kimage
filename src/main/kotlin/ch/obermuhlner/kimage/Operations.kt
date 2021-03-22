package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import kotlin.math.abs

fun deltaRGB(image1: Image, image2: Image, factor: Double = 10.0): Image {
    return MatrixImage(
        image1.width,
        image2.height,
        Channel.Red to (image1[Channel.Red] - image2[Channel.Red]) * factor,
        Channel.Green to (image1[Channel.Green] - image2[Channel.Green]) * factor,
        Channel.Blue to (image1[Channel.Blue] - image2[Channel.Blue]) * factor)
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