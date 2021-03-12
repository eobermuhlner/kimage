package ch.obermuhlner.kimage.image

import ch.obermuhlner.kimage.matrix.CroppedMatrix
import ch.obermuhlner.kimage.matrix.Matrix

class CroppedImage(
        private val image: Image,
        private val offsetX: Int,
        private val offsetY: Int,
        override val width: Int,
        override val height: Int,
        private val strictClipping: Boolean = true)
    : Image {

    override val channels: List<Channel>
        get() = image.channels

    override fun getPixel(x: Int, y: Int, channel: Int): Double {
        return image.getPixel(innerX(x), innerY(y), channel)
    }

    override fun setPixel(x: Int, y: Int, channel: Int, color: Double) {
        image.setPixel(innerX(x), innerY(y), channel, color)
    }

    override fun getMatrix(channel: Channel): Matrix {
        return CroppedMatrix(image.getMatrix(channel), offsetY, offsetX, height, width)
    }

    private fun innerX(x: Int) = if (strictClipping) {
        boundedX(x) + offsetX
    } else {
        x + offsetX
    }

    private fun innerY(y: Int) = if (strictClipping) {
        boundedY(y) + offsetY
    } else {
        y + offsetY
    }
}