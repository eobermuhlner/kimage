package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.matrix.Matrix

open class MatrixImageFilter(private val matrixFilter: (Channel, Matrix) -> Matrix, private val channelFilter: (Channel) -> Boolean = { true }) : Filter<Image> {

    override fun filter(source: Image): Image {
        return MatrixImage(source.width, source.height, source.channels.filter(channelFilter)) { channel, _, _ -> matrixFilter.invoke(channel, source[channel]) }
    }
}