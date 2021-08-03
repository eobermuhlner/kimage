package ch.obermuhlner.kimage.matrix

class CalculatedMatrix(
    override val width: Int,
    override val height: Int,
    private val calculation: (x: Int, y: Int) -> Double
) : Matrix {

    override fun create(createWidth: Int, createHeight: Int): Matrix {
        val m = DoubleMatrix(createWidth, createHeight)
        m.set(this)
        return m
    }

    override fun get(index: Int): Double {
        val y = index / width
        val x = index - y * width
        return get(x, y)
    }

    override fun set(index: Int, value: Double) {
        throw IllegalStateException("CalculatedMatrix cannot set values")
    }

    override fun get(x: Int, y: Int): Double {
        return calculation(x, y)
    }

    override fun set(x: Int, y: Int, value: Double) {
        throw IllegalStateException("CalculatedMatrix cannot set values")
    }

    override operator fun iterator(): Iterator<Double> {
        return CalculatedMatrixIterator(this)
    }

    override fun equals(other: Any?): Boolean = (other is Matrix) && contentEquals(other)

    override fun toString(): String {
        return "CalculatedMatrix($width, $height)"
    }

    private class CalculatedMatrixIterator(val matrix: CalculatedMatrix): DoubleIterator() {
        var index: Int = 0

        override fun hasNext(): Boolean = index < matrix.size-1

        override fun nextDouble(): Double {
            if (index >= matrix.size-1) {
                throw NoSuchElementException("Matrix has ${matrix.size} elements: $index")
            }

            return matrix[index++]
        }
    }
}