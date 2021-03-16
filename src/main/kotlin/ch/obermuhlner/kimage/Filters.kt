package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.matrix.Matrix

fun Image.gaussianBlur(radius: Int): Image = GaussianBlurFilter(radius).filter(this)
fun Matrix.gaussianBlur(radius: Int): Matrix = GaussianBlurFilter.blur(this, radius)

fun Image.average(radius: Int, shape: Shape = Shape.Square): Image = AverageFilter(radius, shape).filter(this)
fun Matrix.average(radius: Int, shape: Shape = Shape.Square): Matrix = AverageFilter.averageMatrix(this, radius, shape)

fun Image.median(radius: Int, shape: Shape = Shape.Square): Image = MedianFilter(radius, shape).filter(this)
fun Matrix.median(radius: Int, shape: Shape = Shape.Square): Matrix = MedianFilter.medianMatrix(this, radius, shape)

fun Image.kernelFilter(kernel: Matrix): Image = KernelFilter(kernel).filter(this)
fun Image.sharpen(): Image = this.kernelFilter(KernelFilter.Sharpen)
fun Image.unsharpMask(): Image = this.kernelFilter(KernelFilter.UnsharpMask)
fun Image.edgeDetectionStrong(): Image = this.kernelFilter(KernelFilter.EdgeDetectionStrong)
fun Image.edgeDetectionCross(): Image = this.kernelFilter(KernelFilter.EdgeDetectionCross)
fun Image.edgeDetectionDiagonal(): Image = this.kernelFilter(KernelFilter.EdgeDetectionDiagonal)

fun Matrix.sharpen(): Matrix = this.convolute(KernelFilter.Sharpen)
fun Matrix.unsharpMask(): Matrix = this.convolute(KernelFilter.UnsharpMask)
fun Matrix.edgeDetectionStrong(): Matrix = this.convolute(KernelFilter.EdgeDetectionStrong)
fun Matrix.edgeDetectionCross(): Matrix = this.convolute(KernelFilter.EdgeDetectionCross)
fun Matrix.edgeDetectionDiagonal(): Matrix = this.convolute(KernelFilter.EdgeDetectionDiagonal)


