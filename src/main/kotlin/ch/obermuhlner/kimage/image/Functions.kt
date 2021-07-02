package ch.obermuhlner.kimage.image

import ch.obermuhlner.kimage.Scaling
import ch.obermuhlner.kimage.io.ImageFormat
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.math.clamp
import ch.obermuhlner.kimage.matrix.*
import java.io.File
import kotlin.math.*

fun deltaRGB(image1: Image, image2: Image, factor: Double = 10.0): Image {
    return MatrixImage(
        image1.width,
        image2.height,
        image1.channels) { channel, _, _ ->
        (image1[channel] - image2[channel]) * factor
    }
}

fun deltaChannel(image1: Image, image2: Image, factor: Double = 5.0, channel: Channel = Channel.Luminance): Image {
    val f = 0.2
    var delta = (image1[channel] - image2[channel]) * factor
    delta = delta.onEach { v -> exaggerate(v) }
    val red = delta.onEach   { v -> if (v < 0.0) -v     else v * f }
    val green = delta.onEach { v -> if (v < 0.0) -v * f else v * f }
    val blue = delta.onEach  { v -> if (v < 0.0) -v * f else v     }
    return MatrixImage(
        image1.width,
        image1.height,
        Channel.Red to red,
        Channel.Green to green,
        Channel.Blue to blue)
}

// exaggerates low values but never reaches 1.0
private fun exaggerate(x: Double): Double = -1/(x+0.5)+2

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

fun max(image1: Image, image2: Image): Image {
    return MatrixImage(
        image1.width,
        image1.height,
        image1.channels) { channel, _, _ ->
        max(image1[channel], image2[channel])
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

fun Image.values(channels: List<Channel> = this.channels): Iterable<Double> =
    ImageValueIterable(this, channels)

private class CompositeIterator<T>(iterators: List<Iterator<T>>): Iterator<T> {
    val iterators = iterators.filter { it.hasNext() } .toMutableList()

    override fun hasNext(): Boolean = iterators.isNotEmpty()

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val element = iterators.first().next()

        if (!iterators.first().hasNext()) {
            iterators.removeFirst()
        }

        return element
    }
}

private class ImageValueIterable(private val image: Image, private val channels: List<Channel>) : Iterable<Double> {
    override fun iterator(): Iterator<Double> = CompositeIterator(channels.map { image[it].iterator() })
}

fun Image.averageError(other: Image, channels: List<Channel> = this.channels): Double {
    var sum = 0.0
    for (channel in channels) {
        sum += this[channel].averageError(other[channel])
    }
    return sum / channels.size.toDouble()
}

fun Image.crop(croppedX: Int, croppedY: Int, croppedWidth: Int, croppedHeight: Int, strictClipping: Boolean = true): Image {
    return CroppedImage(this, croppedX, croppedY, croppedWidth, croppedHeight, strictClipping)
}

fun Image.cropCenter(radius: Int, croppedCenterX: Int = width / 2, croppedCenterY: Int = height / 2, strictClipping: Boolean = true): Image {
    return cropCenter(radius, radius, croppedCenterX, croppedCenterY, strictClipping)
}

fun Image.cropCenter(radiusX: Int, radiusY: Int, croppedCenterX: Int = width / 2, croppedCenterY: Int = height / 2, strictClipping: Boolean = true): Image {
    return crop(croppedCenterX - radiusX, croppedCenterY - radiusY, radiusX*2+1, radiusY*2+1, strictClipping)
}

fun Image.scaleBy(scaleX: Double, scaleY: Double, scaling: Scaling = Scaling.Bicubic): Image {
    val newWidth = (width * scaleX).toInt()
    val newHeight = (height * scaleY).toInt()
    return scaleTo(newWidth, newHeight, scaling)
}

fun Image.scaleTo(newWidth: Int, newHeight: Int, scaling: Scaling = Scaling.Bicubic): Image {
    return MatrixImage(
        newWidth,
        newHeight,
        this.channels) { channel, _, _ ->
        this[channel].scaleTo(newHeight, newWidth, scaling)
    }
}

fun Image.interpolate(fixPoints: List<Pair<Int, Int>>, power: Double = estimatePowerForInterpolation(fixPoints.size)): Image {
    val fixPointsRowColumn = fixPoints.map { Pair(it.second, it.first) }
    val medianRadius = min(width, height) / max(sqrt(fixPoints.size.toDouble()).toInt()+1, 2)
    return MatrixImage(
        width,
        height,
        this.channels) { channel, _, _ ->
        this[channel].interpolate(fixPointsRowColumn,  { medianAround(this[channel], it.first, it.second, medianRadius) }, power = power)
    }
}

fun Image.interpolate(fixPoints: List<Pair<Int, Int>>, fixValues: List<Double>, power: Double = estimatePowerForInterpolation(fixPoints.size)): Image {
    val fixPointsRowColumn = fixPoints.map { Pair(it.second, it.first) }
    return MatrixImage(
        width,
        height,
        this.channels) { channel, _, _ ->
        this[channel].interpolate(fixPointsRowColumn,  fixValues, power = power)
    }
}

fun Image.write(file: File) {
    ImageWriter.write(this, file)
}

fun Image.write(file: File, format: ImageFormat) {
    ImageWriter.write(this, file, format)
}

