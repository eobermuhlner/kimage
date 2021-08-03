package ch.obermuhlner.kimage.matrix

class CroppedMatrix(
    private val matrix: Matrix,
    private val offsetX: Int,
    private val offsetY: Int,
    override val width: Int,
    override val height: Int,
    private val strictClipping: Boolean = true
) : Matrix {

    override fun create(createWidth: Int, createHeight: Int): Matrix {
        return matrix.create(createWidth, createHeight)
    }

    override fun get(index: Int): Double {
        val y = index / width
        val x = index % width
        return get(x, y)
    }

    override fun set(index: Int, value: Double) {
        val y = index / width
        val x = index % width
        set(x, y, value)
    }

    override fun get(x: Int, y: Int): Double {
        return matrix[innerX(x), innerY(y)]
    }

    override fun set(x: Int, y: Int, value: Double) {
        matrix[innerX(x), innerY(y)] = value
    }

    private fun innerY(y: Int) = if (strictClipping) {
        boundedY(y) + offsetY
    } else {
        y + offsetY
    }

    private fun innerX(x: Int) = if (strictClipping) {
        boundedX(x) + offsetX
    } else {
        x + offsetX
    }

    override fun equals(other: Any?): Boolean = (other is Matrix) && contentEquals(other)

    override fun toString(): String {
        return "CroppedMatrix($width, $height, offset=($offsetX, $offsetY), $matrix)"
    }

}