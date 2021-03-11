package ch.obermuhlner.kimage.matrix

class DoubleMatrix(override val rows: Int, override val columns: Int) : Matrix {
    private val data: DoubleArray = DoubleArray(columns * rows)

    override fun create(rows: Int, columns: Int): Matrix = DoubleMatrix(rows, columns)

    override fun get(row: Int, column: Int): Double {
        val index = boundedColumn(column) + boundedRow(row) * columns
        return data[index]
    }

    override fun set(row: Int, column: Int, value: Double) {
        val index = boundedColumn(column) + boundedRow(row) * columns
        data[index] = value
    }

}