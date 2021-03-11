package ch.obermuhlner.kimage.image

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
        val xx = if (strictClipping)  {
            boundedX(x + offsetX)
        } else {
            x + offsetX
        }
        val yy = if (strictClipping) {
            boundedY(y + offsetY)
        } else {
            y + offsetY
        }
        return image.getPixel(xx, yy, channel)
    }

    override fun setPixel(x: Int, y: Int, channel: Int, color: Double) {
        val xx = if (strictClipping)  {
            boundedX(x + offsetX)
        } else {
            x + offsetX
        }
        val yy = if (strictClipping) {
            boundedY(y + offsetY)
        } else {
            y + offsetY
        }
        image.setPixel(xx, yy, channel, color)
    }
}