package ch.obermuhlner.kimage.matrix

class DoubleMatrix(override val rows: Int, override val columns: Int) : Matrix {
    private val data: DoubleArray = DoubleArray(columns * rows)

    override fun create(rows: Int, columns: Int): Matrix = DoubleMatrix(rows, columns)

    override fun get(index: Int): Double {
        return data[index]
    }

    override fun set(index: Int, value: Double) {
        data[index] = value
    }

    override operator fun iterator(): Iterator<Double> {
        return data.iterator()
    }

}