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

    fun fft(matrix: ComplexMatrix): ComplexMatrix {
        return fft(matrix, Complex(0.0, 2.0), 1.0)
    }

    fun fftInverse(matrix: ComplexMatrix): ComplexMatrix {
        return fft(matrix, Complex(0.0, -2.0), 2.0)
    }

    private fun fft(matrix: ComplexMatrix, direction: Complex, scalar: Double): ComplexMatrix {
        val transformed1 = mutableListOf<Array<Complex>>()
        for (y in 0 until matrix.height) {
            val row = Array(matrix.width) { x -> matrix[x, y] }
            val t = fft(row, direction, scalar)
            transformed1.add(t)
        }

        val transformed2 = mutableListOf<Array<Complex>>()
        for (x in 0 until matrix.width) {
            val column = Array(matrix.height) { y -> transformed1[y][x] }
            val t = fft(column, direction, scalar)
            transformed2.add(t)
        }

        val resultMatrix = ComplexMatrix(matrix.width, matrix.height) { x, y ->
            transformed2[x][y]
        }
        return resultMatrix
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

class ComplexMatrix(
    val width: Int,
    val height: Int,
    init: (index: Int) -> Complex = { Complex(0.0) }
) {
    private val data = Array(width * height, init)

    val re: Matrix get() = DoubleMatrix(width, height) { x, y -> this[x, y].re }
    val im: Matrix get() = DoubleMatrix(width, height) { x, y -> this[x, y].im }

    constructor(width: Int, height: Int, init: (x: Int, y: Int) -> Complex) : this(width, height, { index ->
        val x = index % width
        val y = index / width
        init(x, y)
    })

    constructor(matrix: Matrix) : this(matrix.width, matrix.height, { index ->
        Complex(matrix[index])
    })

    operator fun get(x: Int, y: Int): Complex {
        return data[x + y* width]
    }
    operator fun set(x: Int, y: Int, value: Complex) {
        data[x + y* width] = value
    }

    operator fun plus(other: ComplexMatrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] + other[x, y]
        }
    }
    operator fun plus(other: Matrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] + Complex(other[x, y])
        }
    }

    operator fun minus(other: ComplexMatrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] - other[x, y]
        }
    }
    operator fun minus(other: Matrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] - Complex(other[x, y])
        }
    }

    operator fun times(other: ComplexMatrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            var sum = Complex(0.0)
            val thisXY = this[x, y]
            for (otherX in 0 until other.width) {
                sum += thisXY * other[otherX, x]
            }
            sum
        }
    }

    operator fun times(other: Complex): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] * other
        }
    }
    operator fun times(other: Double): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] * other
        }
    }
    operator fun div(other: Complex): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] / other
        }
    }
    operator fun div(other: Double): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] / other
        }
    }

    infix fun elementTimes(other: ComplexMatrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] * other[x, y]
        }
    }
    infix fun elementDiv(other: ComplexMatrix): ComplexMatrix {
        return ComplexMatrix(width, height) { x, y ->
            this[x, y] / other[x, y]
        }
    }

    override fun toString(): String = "ComplexMatrix($width, $height)"
}

class Complex(val re: Double, val im: Double = 0.0) {
    operator fun plus(x: Complex) = Complex(re + x.re, im + x.im)
    operator fun minus(x: Complex) = Complex(re - x.re, im - x.im)
    operator fun times(x: Double) = Complex(re * x, im * x)
    operator fun times(x: Complex) = Complex(re * x.re - im * x.im, re * x.im + im * x.re)
    operator fun div(x: Complex) = this * x.reciprocal
    operator fun div(x: Double) = Complex(re / x, im / x)
    operator fun unaryMinus() = Complex(-re, -im)
    val exp: Complex by lazy { Complex(cos(im), sin(im)) * (cosh(re) + sinh(re)) }
    val absSquare: Double by lazy { re*re + im*im }
    val sqrt: Complex by lazy { Complex(re*re, im*im) }
    val conjugate: Complex by lazy { Complex(re, -im) }
    val reciprocal: Complex by lazy { Complex(re / absSquare, -im / absSquare) }

    override fun toString() = when {
        im == 0.0 -> "$re"
        re == 0.0 -> "${im}i"
        im >= 0.0 -> "$re + ${im}i"
        else -> "$re - ${-im}i"
    }
}
