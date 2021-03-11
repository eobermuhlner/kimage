package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.matrix.Matrix

open class MatrixImageFilter(private val matrixFilter: (Channel, Matrix) -> Matrix, private val channelFilter: (Channel) -> Boolean = { true }) : Filter<MatrixImage> {

    override fun filter(source: MatrixImage): MatrixImage {
        return MatrixImage(source.width, source.height, source.channels.filter(channelFilter)) { channel, _, _ -> matrixFilter.invoke(channel, source.getMatrix(channel)) }
    }
}