package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.math.clamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

interface Matrix : Iterable<Double> {
    val width: Int
    val height: Int

    val size: Int
        get() = width * height

    fun create(createWidth: Int = width, createHeight: Int = height): Matrix

    operator fun get(index: Int): Double
    operator fun set(index: Int, value: Double)

    operator fun get(x: Int, y: Int): Double {
        val index = boundedX(x) + boundedY(y) * width
        return this[index]
    }
    operator fun set(x: Int, y: Int, value: Double) {
        val index = boundedX(x) + boundedY(y) * width
        this[index] = value
    }

    fun set(other: Matrix, offsetX: Int = 0, offsetY: Int = 0) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                this[x+offsetX, y+offsetY] = other[x, y]
            }
        }
    }

    fun isInside(x: Int, y: Int) = y in 0 until height && x in 0 until width

    fun boundedX(x: Int) = clamp(x, 0, width - 1)
    fun boundedY(y: Int) = clamp(y, 0, height - 1)

    override operator fun iterator(): Iterator<Double> {
        return object : Iterator<Double> {
            var index = 0
            override fun hasNext(): Boolean = index < this@Matrix.size
            override fun next(): Double = this@Matrix[index++]
        }
    }

    operator fun plus(other: Matrix): Matrix {
        checkSameSize(this, other)

        val m = create()
        for (y in 0 until height) {
            for (x in 0 until width) {
                m[x, y] = this[x, y] + other[x, y]
            }
        }
        return m
    }

    operator fun plusAssign(other: Matrix) {
        checkSameSize(this, other)

        for (y in 0 until height) {
            for (x in 0 until width) {
                this[x, y] += other[x, y]
            }
        }
    }

    operator fun minus(other: Matrix): Matrix {
        checkSameSize(this, other)

        val m = create()
        for (y in 0 until height) {
            for (x in 0 until width) {
                m[x, y] = this[x, y] - other[x, y]
            }
        }
        return m
    }

    operator fun minusAssign(other: Matrix) {
        checkSameSize(this, other)

        for (y in 0 until height) {
            for (x in 0 until width) {
                this[x, y] -= other[x, y]
            }
        }
    }

    operator fun times(other: Matrix): Matrix {
        checkWidthOtherHeight(this, other)

        val m = create(other.width, this.height)
        for (y in 0 until height) {
            for (otherX in 0 until other.width) {
                var sum = 0.0
                for (x in 0 until width) {
                    sum += this[x, y] * other[otherX, x]
                }
                m[otherX, y] = sum
            }
        }
        return m
    }

    operator fun times(other: Double): Matrix {
        val m = copy()
        m.onEach { value ->
            value * other
        }
        return m
    }

    operator fun timesAssign(other: Double) {
        onEach { value ->
            value * other
        }
    }

    operator fun div(other: Double): Matrix {
        val m = copy()
        m.onEach { value ->
            value / other
        }
        return m
    }

    operator fun divAssign(other: Double) {
        onEach { value ->
            value / other
        }
    }

    operator fun minusAssign(other: Double) {
        onEach { value ->
            value - other
        }
    }

    infix fun elementPlus(other: Double): Matrix {
        return copy().onEach { value ->
            value + other
        }
    }

    infix fun elementMinus(other: Double): Matrix {
        return copy().onEach { value ->
            value - other
        }
    }

    infix fun elementTimes(other: Matrix): Matrix {
        return copy().onEach { x, y, value ->
            value * other[x, y]
        }
    }

    infix fun elementDiv(other: Matrix): Matrix {
        return copy().onEach { x, y, value ->
            value / other[x, y]
        }
    }

    fun convolute(kernel: Matrix): Matrix {
        if (size * kernel.size >= 10000) {
            return convoluteParallel(kernel)
        }

        val m = create()
        for (y in 0 until height) {
            for (x in 0 until width) {
                var value = 0.0
                for (kernelY in 0 until kernel.height) {
                    for (kernelX in 0 until kernel.width) {
                        val pixel = this[x - kernel.width/2 + kernelX, y - kernel.height/2 + kernelY]
                        value += pixel * kernel[kernelX, kernelY]
                    }
                }
                m[x, y] = value
            }
        }
        return m
    }

    fun Matrix.convoluteParallel(kernel: Matrix): Matrix = runBlocking {
        val m = create()
        for (y in 0 until height) {
            launch(Dispatchers.Default) {
                for (x in 0 until width) {
                    ensureActive()
                    var value = 0.0
                    for (kernelY in 0 until kernel.height) {
                        for (kernelX in 0 until kernel.width) {
                            val pixel =
                                this@convoluteParallel[x - kernel.width / 2 + kernelX, y - kernel.height / 2 + kernelY]
                            value += pixel * kernel[kernelX, kernelY]
                        }
                    }
                    m[x, y] = value
                }
            }
        }
        m
    }

    fun transpose(): Matrix {
        val m = create(height, width)

        for (y in 0 until height) {
            for (x in 0 until width) {
                m[x, y] = this[y, x]
            }
        }

        return m
    }

    fun invert(): Matrix {
        checkSquare(this)

        val m = create(width*2, height)
        m.set(this, 0, 0)
        m.set(m.identity(height), width, 0)

        m.gaussianElimination()

        return m.crop(width, 0, width, height)
    }

    fun gaussianElimination(reducedEchelonForm: Boolean = true) {
        var pivotY = 0
        var pivotX = 0

        while (pivotY < height && pivotX < width) {
            var maxY = pivotY
            for (y in pivotY + 1 until height) {
                if (abs(this[pivotX, y]) > this[pivotX, maxY]) {
                    maxY = y
                }
            }
            val pivotCell = this[pivotX, maxY]
            if (pivotCell == 0.0) {
                pivotX++
            } else {
                swapY(pivotY, maxY)
                val divisor = this[pivotX, pivotY]
                for (y in pivotY + 1 until height) {
                    val factor = this[pivotX, y] / divisor
                    this[pivotX, y] = 0.0
                    for (x in pivotX + 1 until width) {
                        val value = this[x, y] - this[x, pivotY] * factor
                        this[x, y] = value
                    }
                }
            }
            if (reducedEchelonForm) {
                val pivotDivisor = this[pivotX, pivotY]
                this[pivotX, pivotY] = 1.0
                for (x in pivotX + 1 until width) {
                    val value = this[x, pivotY] / pivotDivisor
                    this[x, pivotY] =  value
                }
                for (y in 0 until pivotY) {
                    val factor = this[pivotX, y]
                    this[pivotX, y] = 0.0
                    for (x in pivotX + 1 until width) {
                        val value = this[x, y] - this[x, pivotY] * factor
                        this[x, y] = value
                    }
                }
            }
            pivotX++
            pivotY++
        }
    }

    fun swapY(y1: Int, y2: Int) {
        checkY(this, "y1", y1)
        checkY(this, "y2", y2)

        if (y1 == y2) {
            return
        }
        for (x in 0 until width) {
            val tmp = this[x, y1]
            this[x, y1] = this[x, y2]
            this[x, y2] = tmp
        }
    }

    fun swapX(x1: Int, x2: Int) {
        checkX(this, "x1", x1)
        checkX(this, "x2", x2)

        if (x1 == x2) {
            return
        }
        for (y in 0 until width) {
            val tmp = this[x1, y]
            this[x1, y] = this[x2, y]
            this[x2, y] = tmp
        }
    }

    fun identity(size: Int): Matrix {
        return CalculatedMatrix(size, size) { x, y ->
            if (x == y) 1.0 else 0.0
        }
    }

    fun copy(): Matrix {
        val m = create()
        m.set(this)
        return m
    }

    fun onEach(func: (Double) -> Double): Matrix {
        for (index in 0 until size) {
            this[index] = func.invoke(this[index])
        }
        return this
    }

    fun onEach(func: (x: Int, y: Int, value: Double) -> Double): Matrix {
        for (y in 0 until height) {
            for (x in 0 until width) {
                this[x, y] = func.invoke(x, y, this[x, y])
            }
        }
        return this
    }

    fun accumulate(func: (acc: Double, value: Double) -> Double): Double {
        var result = this[0]

        for (index in 1 until size) {
            result = func(result, this[index])
        }

        return result
    }

    fun crop(croppedX: Int, croppedY: Int, croppedWidth: Int, croppedHeight: Int, strictClipping: Boolean = true): Matrix {
        return CroppedMatrix(this, croppedX, croppedY, croppedWidth, croppedHeight, strictClipping)
    }

    fun cropCenter(
        radius: Int,
        croppedCenterX: Int = width / 2,
        croppedCenterY: Int = height / 2,
        strictClipping: Boolean = true
    ): Matrix {
        return cropCenter(radius, radius, croppedCenterX, croppedCenterY, strictClipping)
    }

    fun cropCenter(
        radiusX: Int,
        radiusY: Int,
        croppedCenterX: Int = width / 2,
        croppedCenterY: Int = height / 2,
        strictClipping: Boolean = true
    ): Matrix {
        return crop(croppedCenterX - radiusX, croppedCenterY - radiusY, radiusX*2+1, radiusY*2+1, strictClipping)
    }


    companion object {
        private fun checkHeight(height: Int) {
            require(height >= 0) { "height < 0 : $height" }
        }

        private fun checkWidth(width: Int) {
            require(width >= 0) { "width < 0 : $width" }
        }

        private fun checkSquare(matrix: Matrix) {
            require(matrix.width == matrix.height) {
                "width " + matrix.width.toString() + " != height " + matrix.height
            }
        }

        private fun checkY(matrix: Matrix, y: Int) {
            checkY(matrix, "y", y)
        }

        private fun checkY(matrix: Matrix, name: String, y: Int) {
            checkY(matrix.height, name, y)
        }

        private fun checkY(height: Int, y: Int) {
            checkY(height, "y", y)
        }

        fun checkY(height: Int, name: String, y: Int) {
            require(y >= 0) { "$name < 0 : $y" }
            require(y < height) { "$name >= $height : $y" }
        }

        private fun checkX(matrix: Matrix, x: Int) {
            checkX(matrix, "x", x)
        }

        private fun checkX(matrix: Matrix, name: String, x: Int) {
            require(x >= 0) { "$name < 0 : $x" }
            require(x < matrix.width) { name + " >= " + matrix.width + " : " + x }
        }

        private fun checkSameSize(matrix: Matrix, other: Matrix) {
            require(!(matrix.height != other.height)) {
                "height != other.height : " + matrix.height.toString() + " != " + other.height
            }
            require(!(matrix.width != other.width)) {
                "width != other.width : " + matrix.width.toString() + " != " + other.width
            }
        }

        private fun checkWidthOtherHeight(matrix: Matrix, other: Matrix) {
            require(!(matrix.width != other.height)) {
                "width != other.height : " + matrix.width.toString() + " != " + other.height
            }
        }

        fun matrixOf( width: Int, height: Int, vararg values: Double): Matrix {
            val m = DoubleMatrix(width, height)
            var y = 0
            var x = 0

            for (value in values) {
                m[x, y] = value
                x++
                if (x >= width) {
                    x = 0
                    y++
                    if (y >= height) {
                        break
                    }
                }
            }
            return m
        }

        fun matrixOf(width: Int, height: Int, init: (x: Int, y: Int) -> Double): Matrix {
            return DoubleMatrix(width, height, init)
        }
    }
}