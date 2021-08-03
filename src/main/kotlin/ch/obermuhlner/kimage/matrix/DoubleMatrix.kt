package ch.obermuhlner.kimage.matrix

class DoubleMatrix(override val width: Int, override val height: Int) : Matrix {
    private val data: DoubleArray = DoubleArray(width * height)

    constructor(width: Int, height: Int, init: (x: Int, y: Int) -> Double): this(width, height) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                this[x, y] = init(x, y)
            }
        }
    }

    override fun create(createWidth: Int, createHeight: Int): Matrix = DoubleMatrix(createWidth, createHeight)

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
        return "DoubleMatrix($width, $height)"
    }

}