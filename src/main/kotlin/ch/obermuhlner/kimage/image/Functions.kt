package ch.obermuhlner.kimage.image

import ch.obermuhlner.kimage.Scaling
import ch.obermuhlner.kimage.io.ImageFormat
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.math.Histogram
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
    val red = delta.copy().onEach   { v -> if (v < 0.0) -v     else v * f }
    val green = delta.copy().onEach { v -> if (v < 0.0) -v * f else v * f }
    val blue = delta.copy().onEach  { v -> if (v < 0.0) -v * f else v     }
    return MatrixImage(
        image1.width,
        image1.height,
        Channel.Red to red,
        Channel.Green to green,
        Channel.Blue to blue)
}

// exaggerates low values but never reaches 1.0
fun exaggerate(x: Double): Double = -1/(x+0.5)+2

fun Image.histogramImage(
    histogramWidth: Int,
    histogramHeight: Int,
    scaleYFunction: (Double) -> Double = { x: Double -> x },
    histogramFunction: (Channel) -> Histogram = { Histogram(histogramWidth) },
    channels: List<Channel> = this.channels,
    ignoreMinMaxBins: Boolean = true
): Image {
    val result = MatrixImage(histogramWidth, histogramHeight)

    val channelHistograms = mutableMapOf<Channel, Histogram>()
    var maxCount = 0
    for (channel in channels) {
        val histogram = histogramFunction(channel)
        channelHistograms[channel] = histogram

        this[channel].forEach { histogram.add(it) }
        maxCount = max(maxCount, histogram.max(ignoreMinMaxBins))
    }

    val max = scaleYFunction(maxCount.toDouble())
    for (channel in channels) {
        val histogram = channelHistograms[channel]!!

        for (x in 0 until histogramWidth) {
            val histY = min(histogramHeight, (histogramHeight.toDouble() * scaleYFunction(histogram[x].toDouble()) / max).toInt())
            for (y in (histogramHeight-histY) until histogramHeight) {
                result[channel][x, y] = 1.0
            }
        }
    }
    
    return result
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

fun Image.elementPlus(value: Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].elementPlus(value)
    }
}

fun Image.elementMinus(value: Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].elementMinus(value)
    }
}

// pixel-wise multiplication
operator fun Image.times(other: Image): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].copy().onEach { x, y, value ->
            value * other[channel][x, y]
        }
    }
}

// pixel-wise division
operator fun Image.div(other: Image): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].copy().onEach { x, y, value ->
            value / other[channel][x, y]
        }
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

operator fun Image.plus(value: Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel] elementPlus value
    }
}

operator fun Image.minus(value: Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel] elementMinus value
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

fun Image.onEach(func: (value: Double) -> Double): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].onEach(func)
    }
}

fun Image.rotateLeft(): Image {
    return MatrixImage(
        this.height,
        this.width,
        this.channels) { channel, _, _ ->
        this[channel].rotateLeft()
    }
}

fun Image.rotateRight(): Image {
    return MatrixImage(
        this.height,
        this.width,
        this.channels) { channel, _, _ ->
        this[channel].rotateRight()
    }
}

fun Image.mirrorX(): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].mirrorX()
    }
}

fun Image.mirrorY(): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].mirrorY()
    }
}

fun Image.stretchClassic(min: Double, max: Double, func: (value: Double) -> Double = { it }): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].stretchClassic(min, max, func)
    }
}

fun Image.stretchPercentile(low: Double, high: Double, channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue), func: (value: Double) -> Double = { it }): Image {
    val histogram = Histogram()
    for (measureChannel in channels) {
        histogram.add(this[measureChannel])
    }

    val lowValue = histogram.estimatePercentile(low)
    val highValue = histogram.estimatePercentile(high)

    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].stretchClassic(lowValue, highValue, func)
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
    return crop(croppedCenterX - radiusX, croppedCenterY - radiusY, radiusX*2, radiusY*2, strictClipping)
}

fun Image.copy(): Image {
    return MatrixImage(this.width, this.height, this.channels) { channel, width, height ->
        this[channel].copy()
    }
}

fun Image.scaleBy(
    scaleX: Double,
    scaleY: Double,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    scaling: Scaling = Scaling.Bicubic
): Image {
    val newWidth = (width * scaleX).toInt()
    val newHeight = (height * scaleY).toInt()
    return scaleTo(newWidth, newHeight, offsetX, offsetY, scaling)
}

fun Image.scaleTo(
    newWidth: Int,
    newHeight: Int,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    scaling: Scaling = Scaling.Bicubic
): Image {
    return MatrixImage(
        newWidth,
        newHeight,
        this.channels) { channel, _, _ ->
        this[channel].scaleTo(newWidth, newHeight, offsetX, offsetY, scaling)
    }
}

fun Image.scaleToKeepRatio(
    newWidth: Int,
    newHeight: Int,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    scaling: Scaling = Scaling.Bicubic
): Image {
    val ratio = this.width.toDouble() / this.height
    val correctedNewWidth = min(newWidth, (newHeight*ratio).toInt())
    val correctedNewHeight = min(newHeight, (newWidth/ratio).toInt())
    return scaleTo(correctedNewWidth, correctedNewHeight, offsetX, offsetY, scaling)
}

fun Image.interpolate(fixPoints: List<Pair<Int, Int>>, medianRadius: Int = min(width, height) / max(sqrt(fixPoints.size.toDouble()).toInt()+1, 2), power: Double = estimatePowerForInterpolation(fixPoints.size)): Image {
    return MatrixImage(
        width,
        height,
        this.channels) { channel, _, _ ->
        this[channel].interpolate(fixPoints,  { this[channel].medianAround(it.first, it.second, medianRadius) }, power = power)
    }
}

fun Image.interpolate(fixPoints: List<Pair<Int, Int>>, fixValues: List<Double>, power: Double = estimatePowerForInterpolation(fixPoints.size)): Image {
    return MatrixImage(
        width,
        height,
        this.channels) { channel, _, _ ->
        this[channel].interpolate(fixPoints,  fixValues, power = power)
    }
}

fun Image.erode(kernelRadius: Int = 1): Image {
    val hue = this[Channel.Hue]
    val saturation = this[Channel.Saturation]
    val brightness = this[Channel.Brightness].erode(kernelRadius)

    val hsbImage = MatrixImage(this.width, this.height,
        Channel.Hue to hue,
        Channel.Saturation to saturation,
        Channel.Brightness to brightness)

    return MatrixImage(this.width, this.height,
        Channel.Red to hsbImage[Channel.Red],
        Channel.Green to hsbImage[Channel.Green],
        Channel.Blue to hsbImage[Channel.Blue])
}

fun Image.erode(kernel: Matrix, strength: Double = 1.0, repeat: Int = 1): Image {
    val hue = this[Channel.Hue]
    val saturation = this[Channel.Saturation]
    val brightness = this[Channel.Brightness].erode(kernel, strength, repeat)

    val hsbImage = MatrixImage(this.width, this.height,
        Channel.Hue to hue,
        Channel.Saturation to saturation,
        Channel.Brightness to brightness)

    return MatrixImage(this.width, this.height,
        Channel.Red to hsbImage[Channel.Red],
        Channel.Green to hsbImage[Channel.Green],
        Channel.Blue to hsbImage[Channel.Blue])
}

fun Image.processMatrixTiles(size: Int, overlap: Int = size / 4, gradient: (Double) -> Double = { x -> 1.0 - x }, func: (Matrix) -> Matrix): Image {
    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        this[channel].processTiles(size, overlap, gradient, func)
    }
}

fun Image.processImageTiles(size: Int, overlap: Int = size / 4, gradient: (Double) -> Double = { x -> 1.0 - x }, func: (Image) -> Image): Image {
    require (size > overlap) { "Overlap too large" }

    val resultChannelMatrixes = this.channels.associateWith { _ -> Matrix.matrixOf(this.width, this.height) }

    val overlay = Matrix.matrixOf(this.width, this.height) { x, y ->
        val rx = if (x < overlap) {
            (overlap - x.toDouble()) / overlap
        } else if (x > size - overlap) {
            (overlap - (size-x).toDouble()) / overlap
        } else {
            0.0
        }
        val ry = if (y < overlap) {
            (overlap - y.toDouble()) / overlap
        } else if (y > size - overlap) {
            (overlap - (size-y).toDouble()) / overlap
        } else {
            0.0
        }
        clamp(gradient(rx), 0.0, 1.0) * clamp(gradient(ry), 0.0, 1.0)
    }

    val step = size - overlap
    for (tileY in -overlap until this.height step step) {
        for (tileX in -overlap until this.width step step) {
            val croppedImage = this.crop(tileX, tileY, size, size)
            val processedImage = func(croppedImage)
            for (channel in this.channels) {
                val resultChannelMatrix = resultChannelMatrixes[channel]!!
                val processedMatrix = processedImage[channel]
                val m = processedMatrix elementTimes overlay
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val xx = tileX + x
                        val yy = tileY + y
                        if (resultChannelMatrix.isInside(xx, yy)) {
                            resultChannelMatrix[xx, yy] = clamp(resultChannelMatrix[xx, yy] + m[x, y], 0.0, 1.0)
                        }
                    }
                }
            }
        }
    }

    return MatrixImage(
        this.width,
        this.height,
        this.channels) { channel, _, _ ->
        resultChannelMatrixes[channel]!!
    }
}


fun Image.write(file: File) {
    ImageWriter.write(this, file)
}

fun Image.write(file: File, format: ImageFormat) {
    ImageWriter.write(this, file, format)
}

