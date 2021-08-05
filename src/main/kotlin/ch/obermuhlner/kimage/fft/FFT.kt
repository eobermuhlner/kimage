package ch.obermuhlner.kimage.fft

import ch.obermuhlner.kimage.matrix.CalculatedMatrix
import ch.obermuhlner.kimage.matrix.DoubleMatrix
import ch.obermuhlner.kimage.matrix.Matrix
import kotlin.math.*

object FFT {
    fun padPowerOfTwo(matrix: Matrix): Matrix {
        val s = max(matrix.width, matrix.height)

        var p = 1
        while (p < s) {
            p *= 2
        }
        return matrix.crop(0, 0, p, p)
    }

    fun fft(matrix: Matrix): Pair<Matrix, Matrix> {
        return fft(matrix, CalculatedMatrix(matrix.width, matrix.height) { _, _ -> 0.0 })
    }

    fun fft(matrixRe: Matrix, matrixIm: Matrix): Pair<Matrix, Matrix> {
        return fft(matrixRe, matrixIm, Complex(0.0, 2.0), 1.0)
    }

    fun fftInverse(matrixRe: Matrix, matrixIm: Matrix): Pair<Matrix, Matrix> {
        return fft(matrixRe, matrixIm, Complex(0.0, -2.0), 2.0)
    }

    private fun fft(matrixRe: Matrix, matrixIm: Matrix, direction: Complex, scalar: Double): Pair<Matrix, Matrix> {
        val transformed1 = mutableListOf<Array<Complex>>()
        for (y in 0 until matrixRe.height) {
            val row = Array(matrixRe.width) { x -> Complex(matrixRe[x, y], matrixIm[x, y]) }
            val t = fft(row, direction, scalar)
            transformed1.add(t)
        }

        val transformed2 = mutableListOf<Array<Complex>>()
        for (x in 0 until matrixRe.width) {
            val column = Array(matrixRe.height) { y -> transformed1[y][x] }
            val t = fft(column, direction, scalar)
            transformed2.add(t)
        }

        val resultMatrixRe = DoubleMatrix(matrixRe.width, matrixRe.height) { x, y ->
            transformed2[x][y].re
        }
        val resultMatrixIm = DoubleMatrix(matrixRe.width, matrixRe.height) { x, y ->
            transformed2[x][y].im
        }
        return Pair(resultMatrixRe, resultMatrixIm)
    }

    fun fft(arrayRe: Array<Double>): Pair<Array<Double>, Array<Double>> {
        return fft(arrayRe, Array(arrayRe.size) { 0.0 })
    }

    fun fft(arrayRe: Array<Double>, arrayIm: Array<Double>): Pair<Array<Double>, Array<Double>> {
        val resultComplex = fft(Array(arrayRe.size) { i -> Complex(arrayRe[i], arrayIm[i]) })
        return Pair(
            Array(resultComplex.size) { i -> resultComplex[i].re },
            Array(resultComplex.size) { i -> resultComplex[i].im }
        )
    }

    fun fftInverse(arrayRe: Array<Double>, arrayIm: Array<Double>): Pair<Array<Double>, Array<Double>> {
        val resultComplex = fftInverse(Array(arrayRe.size) { i -> Complex(arrayRe[i], arrayIm[i]) })
        return Pair(
            Array(resultComplex.size) { i -> resultComplex[i].re },
            Array(resultComplex.size) { i -> resultComplex[i].im }
        )
    }

    private fun fft(a: Array<Complex>) = fft(a, Complex(0.0, 2.0), 1.0)
    private fun fftInverse(a: Array<Complex>) = fft(a, Complex(0.0, -2.0), 2.0)

    private fun fft(a: Array<Complex>, direction: Complex, scalar: Double): Array<Complex> =
        if (a.size == 1)
            a
        else {
            val n = a.size
            require(n % 2 == 0) { "The Cooley-Tukey FFT algorithm only works when the length of the input is even." }

            var (evens, odds) = Pair(emptyArray<Complex>(), emptyArray<Complex>())
            for (i in a.indices)
                if (i % 2 == 0) evens += a[i]
                else odds += a[i]
            evens = fft(evens, direction, scalar)
            odds = fft(odds, direction, scalar)

            val pairs = (0 until n / 2).map {
                val offset = (direction * (java.lang.Math.PI * it / n)).exp * odds[it] / scalar
                val base = evens[it] / scalar
                Pair(base + offset, base - offset)
            }
            var (left, right) = Pair(emptyArray<Complex>(), emptyArray<Complex>())
            for ((l, r) in pairs) {
                left += l; right += r
            }
            left + right
        }
}

class Complex(val re: Double, val im: Double = 0.0) {
    infix operator fun plus(x: Complex) = Complex(re + x.re, im + x.im)
    infix operator fun minus(x: Complex) = Complex(re - x.re, im - x.im)
    infix operator fun times(x: Double) = Complex(re * x, im * x)
    infix operator fun times(x: Complex) = Complex(re * x.re - im * x.im, re * x.im + im * x.re)
    infix operator fun div(x: Double) = Complex(re / x, im / x)
    val exp: Complex by lazy { Complex(cos(im), sin(im)) * (cosh(re) + sinh(re)) }

    override fun toString() = when {
        im == 0.0 -> "$re"
        re == 0.0 -> "${im}i"
        im >= 0.0 -> "$re + {$im}i"
        else -> "$re - {$im}i"
    }
}
