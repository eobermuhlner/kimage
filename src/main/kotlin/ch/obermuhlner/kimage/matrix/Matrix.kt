package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.math.clamp
import kotlin.math.max
import kotlin.math.sqrt

interface Matrix : Iterable<Double> {
    val rows: Int
    val columns: Int

    val size: Int
        get() = rows * columns

    fun create(createRows: Int = rows, createColumns: Int = columns): Matrix

    operator fun get(index: Int): Double
    operator fun set(index: Int, value: Double)

    operator fun get(row: Int, column: Int): Double {
        val index = boundedColumn(column) + boundedRow(row) * columns
        return this[index]
    }
    operator fun set(row: Int, column: Int, value: Double) {
        val index = boundedColumn(column) + boundedRow(row) * columns
        this[index] = value
    }

    fun set(other: Matrix) {
        checkSameSize(this, other)

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] = other[row, column]
            }
        }
    }

    fun boundedColumn(column: Int) = clamp(column, 0, columns - 1)
    fun boundedRow(row: Int) = clamp(row, 0, rows - 1)

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
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                m[row, column] = this[row, column] + other[row, column]
            }
        }
        return m
    }

    operator fun plusAssign(other: Matrix) {
        checkSameSize(this, other)

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] += other[row, column]
            }
        }
    }

    operator fun minus(other: Matrix): Matrix {
        checkSameSize(this, other)

        val m = create()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                m[row, column] = this[row, column] - other[row, column]
            }
        }
        return m
    }

    operator fun minusAssign(other: Matrix) {
        checkSameSize(this, other)

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] -= other[row, column]
            }
        }
    }

    operator fun times(other: Matrix): Matrix {
        checkColumnsOtherRows(this, other)

        val m = create()
        for (row in 0 until rows) {
            for (otherColumn in 0 until other.columns) {
                var sum = 0.0
                for (column in 0 until columns) {
                    sum += this[row, column] * other[column, otherColumn]
                }
                m[row, otherColumn] = sum
            }
        }
        return m
    }

    operator fun times(value: Double): Matrix {
        val m = create()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                m[row, column] = this[row, column] * value
            }
        }
        return m
    }

    operator fun timesAssign(value: Double) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] *= value
            }
        }
    }

    operator fun div(value: Double): Matrix {
        val m = create()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                m[row, column] = this[row, column] / value
            }
        }
        return m
    }

    operator fun divAssign(value: Double) {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] /= value
            }
        }
    }

    fun convolute(kernel: Matrix): Matrix {
        val m = create()
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                var value = 0.0
                for (kernelRow in 0 until kernel.rows) {
                    for (kernelColumn in 0 until kernel.columns) {
                        val pixel = this[row - kernel.rows/2 + kernelRow, column - kernel.columns/2 + kernelColumn]
                        value += pixel * kernel[kernelRow, kernelColumn]
                    }
                }
                m[row, column] = value
            }
        }
        return m
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
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                this[row, column] = func.invoke(row, column, this[row, column])
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

    fun croppedMatrix(croppedRow: Int, croppedColumn: Int, croppedRows: Int, croppedColumns: Int, strictClipping: Boolean = true): Matrix {
        return CroppedMatrix(this, croppedRow, croppedColumn, croppedRows, croppedColumns, strictClipping)
    }

    companion object {
        private fun checkRows(rows: Int) {
            require(rows >= 0) { "rows < 0 : $rows" }
        }

        private fun checkColumns(columns: Int) {
            require(columns >= 0) { "columns < 0 : $columns" }
        }

        private fun checkSquare(matrix: Matrix) {
            require(matrix.columns == matrix.rows) {
                "columns " + matrix.columns.toString() + " != rows " + matrix.rows
            }
        }

        private fun checkRow(matrix: Matrix, row: Int) {
            checkRow(matrix, "row", row)
        }

        private fun checkRow(matrix: Matrix, name: String, row: Int) {
            checkRow(matrix.rows, name, row)
        }

        private fun checkRow(rows: Int, row: Int) {
            checkRow(rows, "row", row)
        }

        fun checkRow(rows: Int, name: String, row: Int) {
            require(row >= 0) { "$name < 0 : $row" }
            require(row < rows) { "$name >= $rows : $row" }
        }

        private fun checkColumn(matrix: Matrix, column: Int) {
            checkColumn(matrix, "column", column)
        }

        private fun checkColumn(matrix: Matrix, name: String, column: Int) {
            require(column >= 0) { "$name < 0 : $column" }
            require(column < matrix.columns) { name + " >= " + matrix.columns + " : " + column }
        }

        private fun checkSameSize(matrix: Matrix, other: Matrix) {
            require(!(matrix.rows != other.rows)) {
                "rows != other.rows : " + matrix.rows.toString() + " != " + other.rows
            }
            require(!(matrix.columns != other.columns)) {
                "columns != other.columns : " + matrix.columns.toString() + " != " + other.columns
            }
        }

        private fun checkColumnsOtherRows(matrix: Matrix, other: Matrix) {
            require(!(matrix.columns != other.rows)) {
                "columns != other.rows : " + matrix.columns.toString() + " != " + other.rows
            }
        }

        fun matrixOf(rows: Int, columns: Int, vararg values: Double): Matrix {
            val m = DoubleMatrix(rows, columns)
            var row = 0
            var column = 0

            for (value in values) {
                m[row, column] = value
                column++
                if (column >= columns) {
                    column = 0
                    row++
                    if (row >= rows) {
                        break
                    }
                }
            }
            return m
        }
    }
}