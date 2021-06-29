package ch.obermuhlner.kimage.matrix

class FloatMatrix(override val rows: Int, override val columns: Int) : Matrix {
    private val data: FloatArray = FloatArray(columns * rows)

    override fun create(createRows: Int, createColumns: Int): Matrix = FloatMatrix(createRows, createColumns)

    override fun get(index: Int): Double {
        return data[index].toDouble()
    }

    override fun set(index: Int, value: Double) {
        data[index] = value.toFloat()
    }

    override fun toString(): String {
        return "FloatMatrix($rows, $columns)"
    }
}