package ch.obermuhlner.kimage.matrix

class CroppedMatrix(
        private val matrix: Matrix,
        private val offsetRow: Int,
        private val offsetColumn: Int,
        override val rows: Int,
        override val columns: Int,
        private val strictClipping: Boolean = true)
    : Matrix {

    override fun create(r: Int, c: Int): Matrix {
        return matrix.create(r, c)
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