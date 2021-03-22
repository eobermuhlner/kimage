package ch.obermuhlner.kimage.image

import ch.obermuhlner.kimage.matrix.FloatMatrix
import ch.obermuhlner.kimage.matrix.Matrix

class MatrixImage(
        width: Int,
        height: Int,
        channels: List<Channel>,
        channelMatrices: List<Matrix>)
    : AbstractImage(width, height, channels) {

    private val data = Array(channels.size) { channelMatrices[it] }

    constructor(
            width: Int,
            height: Int)
            : this(width, height, Channel.Red, Channel.Green, Channel.Blue)

    constructor(
            width: Int,
            height: Int,
            vararg channels: Channel)
            : this(width, height, channels.toList())

    constructor(
            width: Int,
            height: Int,
            vararg channels: Pair<Channel, Matrix>)
            : this(width, height, channels.map { it.first }, channels.map { it.second })

    constructor(
            width: Int,
            height: Int,
            channels: List<Channel>,
            matrixFunc: (Channel, Int, Int) -> Matrix = { _, _, _ -> FloatMatrix(height, width) })
            : this(width, height, channels, channels.map { matrixFunc.invoke(it, width, height) })

    override fun getPixel(x: Int, y: Int, channel: Channel): Double {
        val channelIndex = channelIndex(channel)
        if (channelIndex < 0) {
            return convertPixelToChannel(x, y, channel)
        }
        return data[channelIndex][y, x]
    }

    override fun setPixel(x: Int, y: Int, channel: Channel, color: Double) {
        data[channelIndex(channel)][y, x] = color
    }

    override fun getMatrix(channel: Channel): Matrix {
        val idx = channelIndex(channel)
        return if (idx >= 0) {
            data[idx]
        } else {
            super.getMatrix(channel)
        }
    }
}