package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.matrix.Matrix

fun Image.gaussianBlurFilter(radius: Int): Image = GaussianBlurFilter(radius).filter(this)
fun Matrix.gaussianBlurFilter(radius: Int): Matrix = GaussianBlurFilter.blur(this, radius)

fun Image.averageFilter(radius: Int, shape: Shape = Shape.Square): Image = AverageFilter(radius, shape).filter(this)
fun Matrix.averageFilter(radius: Int, shape: Shape = Shape.Square): Matrix = AverageFilter.averageMatrix(this, radius, shape)

fun Image.medianFilter(radius: Int, recursive: Boolean = false): Image = FastMedianFilter(radius, recursive).filter(this)
fun Matrix.medianFilter(radius: Int, recursive: Boolean = false): Matrix = FastMedianFilter.fastMedianMatrix(this, radius, recursive)

fun Image.medianPixelFilter(radius: Int): Image = FastMedianFilter(radius).filter(this)

fun Image.slowMedianFilter(radius: Int, shape: Shape = Shape.Square, recursive: Boolean = false): Image = MedianFilter(radius, shape, recursive).filter(this)
fun Matrix.slowMedianFilter(radius: Int, shape: Shape = Shape.Square): Matrix = MedianFilter.medianMatrix(this, radius, shape)

fun Image.kernelFilter(kernel: Matrix): Image = KernelFilter(kernel).filter(this)
fun Image.sharpenFilter(): Image = this.kernelFilter(KernelFilter.Sharpen)
fun Image.unsharpMaskFilter(): Image = this.kernelFilter(KernelFilter.UnsharpMask)
fun Image.edgeDetectionStrongFilter(): Image = this.kernelFilter(KernelFilter.EdgeDetectionStrong)
fun Image.edgeDetectionCrossFilter(): Image = this.kernelFilter(KernelFilter.EdgeDetectionCross)
fun Image.edgeDetectionDiagonalFilter(): Image = this.kernelFilter(KernelFilter.EdgeDetectionDiagonal)

fun Matrix.sharpenFilter(): Matrix = this.convolute(KernelFilter.Sharpen)
fun Matrix.unsharpMaskFilter(): Matrix = this.convolute(KernelFilter.UnsharpMask)
fun Matrix.edgeDetectionStrongFilter(): Matrix = this.convolute(KernelFilter.EdgeDetectionStrong)
fun Matrix.edgeDetectionCrossFilter(): Matrix = this.convolute(KernelFilter.EdgeDetectionCross)
fun Matrix.edgeDetectionDiagonalFilter(): Matrix = this.convolute(KernelFilter.EdgeDetectionDiagonal)

