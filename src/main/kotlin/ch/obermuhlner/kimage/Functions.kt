package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.matrix.stretchClassic
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
    val red = delta.onEach { v -> if (v < 0.0) -v * factor else v * factor * 0.5 }
    val green = delta.onEach { v -> abs(v) * factor * 0.5 }
    val blue = delta.onEach { v -> if (v > 0.0) v * factor else -v * factor * 0.5 }
    return MatrixImage(
        image1.width,
        image2.height,
        Channel.Red to red,
        Channel.Green to green,
        Channel.Blue to blue)
}

operator fun Image.plus(other: Image): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel] + other[channel]
    }
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

operator fun Image.div(value: Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel] / value
    }
}

fun Image.onEach(func: (Double) -> Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].onEach(func)
    }
}

fun Image.stretchClassic(min: Double, max: Double, func: (Double) -> Double = { it }): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].stretchClassic(min, max, func)
    }
}
