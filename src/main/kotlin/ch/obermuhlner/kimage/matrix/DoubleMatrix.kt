package ch.obermuhlner.kimage.matrix

class DoubleMatrix(override val rows: Int, override val columns: Int) : Matrix {
    private val data: DoubleArray = DoubleArray(columns * rows)

    constructor(rows: Int, columns: Int, init: (row: Int, column: Int) -> Double): this(rows, columns) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] = init(row, column)
            }
        }
    }

    override fun create(createRows: Int, createColumns: Int): Matrix = DoubleMatrix(createRows, createColumns)

    override fun get(index: Int): Double {
        return data[index]
    }

    override fun set(index: Int, value: Double) {
        data[index] = value
    }

    override operator fun iterator(): Iterator<Double> {
        return data.iterator()
    }

    override fun equals(other: Any?): Boolean = (other is Matrix) && contentEquals(other)

    override fun toString(): String {
        return "DoubleMatrix($rows, $columns)"
    }

}