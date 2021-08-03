package ch.obermuhlner.kimage.matrix

class FloatMatrix(override val width: Int, override val height: Int) : Matrix {
    private val data: FloatArray = FloatArray(width * height)

    override fun create(createWidth: Int, createHeight: Int): Matrix = FloatMatrix(createWidth, createHeight)

    override fun get(index: Int): Double {
        return data[index].toDouble()
    }

    override fun set(index: Int, value: Double) {
        data[index] = value.toFloat()
    }

    override fun equals(other: Any?): Boolean = (other is Matrix) && contentEquals(other)

    override fun toString(): String {
        return "FloatMatrix($width, $height)"
    }
}