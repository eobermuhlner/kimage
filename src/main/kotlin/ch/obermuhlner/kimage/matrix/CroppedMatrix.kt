package ch.obermuhlner.kimage.matrix

class CroppedMatrix(
        private val matrix: Matrix,
        private val offsetRow: Int,
        private val offsetColumn: Int,
        override val rows: Int,
        override val columns: Int,
        private val strictClipping: Boolean = true)
    : Matrix {

    override fun create(createRows: Int, createColumns: Int): Matrix {
        return matrix.create(createRows, createColumns)
    }

    override fun get(index: Int): Double {
        val row = index / columns
        val column = index % columns
        return get(row, column)
    }

    override fun set(index: Int, value: Double) {
        val row = index / columns
        val column = index % columns
        set(row, column, value)
    }

    override fun get(row: Int, column: Int): Double {
        return matrix.get(innerRow(row), innerColumn(column))
    }

    override fun set(row: Int, column: Int, value: Double) {
        matrix.set(innerRow(row), innerColumn(column), value)
    }

    private fun innerRow(row: Int) = if (strictClipping) {
        boundedRow(row) + offsetRow
    } else {
        row + offsetRow
    }

    private fun innerColumn(column: Int) = if (strictClipping) {
        boundedColumn(column) + offsetColumn
    } else {
        column + offsetColumn
    }
}