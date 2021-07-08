package ch.obermuhlner.kimage.matrix

class CalculatedMatrix(override val rows: Int, override val columns: Int, private val calculation: (row: Int, column: Int) -> Double) : Matrix {

    override fun create(createRows: Int, createColumns: Int): Matrix {
        val m = DoubleMatrix(createRows, createColumns)
        m.set(this)
        return m
    }

    override fun get(index: Int): Double {
        val row = index / columns
        val column = index - row * columns
        return get(row, column)
    }

    override fun set(index: Int, value: Double) {
        throw IllegalStateException("CalculatedMatrix cannot set values")
    }

    override fun get(row: Int, column: Int): Double {
        return calculation(row, column)
    }

    override fun set(row: Int, column: Int, value: Double) {
        throw IllegalStateException("CalculatedMatrix cannot set values")
    }

    override operator fun iterator(): Iterator<Double> {
        return CalculatedMatrixIterator(this)
    }

    override fun equals(other: Any?): Boolean = (other is Matrix) && contentEquals(other)

    override fun toString(): String {
        return "CalculatedMatrix($rows, $columns)"
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