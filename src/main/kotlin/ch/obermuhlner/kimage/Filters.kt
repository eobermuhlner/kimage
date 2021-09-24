package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.sqrt

fun Image.gaussianBlurFilter(radius: Int): Image = GaussianBlurFilter(radius).filter(this)
fun Matrix.gaussianBlurFilter(radius: Int): Matrix = GaussianBlurFilter.blur(this, radius)

fun Image.averageFilter(radius: Int, shape: Shape = Shape.Square): Image = AverageFilter(radius, shape).filter(this)
fun Matrix.averageFilter(radius: Int, shape: Shape = Shape.Square): Matrix = AverageFilter.averageMatrix(this, radius, shape)

fun Image.medianFilter(radius: Int, recursive: Boolean = false): Image = FastMedianFilter(radius, recursive).filter(this)
fun Matrix.medianFilter(radius: Int, recursive: Boolean = false): Matrix = FastMedianFilter.fastMedianMatrix(this, radius, recursive)

fun Image.medianPixelFilter(radius: Int): Image = FastMedianFilter(radius).filter(this)

fun Image.slowMedianFilter(radius: Int, shape: Shape = Shape.Square, recursive: Boolean = false): Image = MedianFilter(radius, shape, recursive).filter(this)
fun Matrix.slowMedianFilter(radius: Int, shape: Shape = Shape.Square): Matrix = MedianFilter.medianMatrix(this, radius, shape)

fun Image.unsharpMaskFilter(radius: Int, strength: Double): Image = MatrixImageFilter({ _, matrix -> matrix.unsharpMaskFilter(radius, strength) }).filter(this)
fun Matrix.unsharpMaskFilter(radius: Int, strength: Double): Matrix {
    val blurred = this.gaussianBlurFilter(radius)
    val m = (this - blurred) * strength
    return this + m
}

fun Image.kernelFilter(kernel: Matrix): Image = KernelFilter(kernel).filter(this)
fun Image.sharpenFilter(): Image = this.kernelFilter(KernelFilter.Sharpen)
fun Image.edgeDetectionStrongFilter(): Image = this.kernelFilter(KernelFilter.EdgeDetectionStrong)
fun Image.edgeDetectionCrossFilter(): Image = this.kernelFilter(KernelFilter.EdgeDetectionCross)
fun Image.edgeDetectionDiagonalFilter(): Image = this.kernelFilter(KernelFilter.EdgeDetectionDiagonal)

fun Matrix.sharpenFilter(): Matrix = this.convolute(KernelFilter.Sharpen)
fun Matrix.unsharpMaskFilter(): Matrix = this.convolute(KernelFilter.UnsharpMask)
fun Matrix.edgeDetectionStrongFilter(): Matrix = this.convolute(KernelFilter.EdgeDetectionStrong)
fun Matrix.edgeDetectionCrossFilter(): Matrix = this.convolute(KernelFilter.EdgeDetectionCross)
fun Matrix.edgeDetectionDiagonalFilter(): Matrix = this.convolute(KernelFilter.EdgeDetectionDiagonal)

fun Image.sobelFilter(sobelHorizontal: Matrix = KernelFilter.SobelHorizontal3, sobelVertical: Matrix = KernelFilter.SobelVertical3): Image = MatrixImageFilter({ _, matrix -> matrix.sobelFilter(sobelHorizontal, sobelVertical) }).filter(this)
fun Matrix.sobelFilter(sobelHorizontal: Matrix = KernelFilter.SobelHorizontal3, sobelVertical: Matrix = KernelFilter.SobelVertical3): Matrix {
    val gx = this.convolute(sobelHorizontal)
    val gy = this.convolute(sobelVertical)

    return Matrix.matrixOf(width, height) { x, y ->
        val px = gx[x, y]
        val py = gy[x, y]
        sqrt(px*px + py*py)
    }
}

fun Image.sobelFilter3() = this.sobelFilter(KernelFilter.SobelHorizontal3, KernelFilter.SobelVertical3)
fun Matrix.sobelFilter3() = this.sobelFilter(KernelFilter.SobelHorizontal3, KernelFilter.SobelVertical3)
fun Image.sobelFilter5() = this.sobelFilter(KernelFilter.SobelHorizontal5, KernelFilter.SobelVertical5)
fun Matrix.sobelFilter5() = this.sobelFilter(KernelFilter.SobelHorizontal5, KernelFilter.SobelVertical5)