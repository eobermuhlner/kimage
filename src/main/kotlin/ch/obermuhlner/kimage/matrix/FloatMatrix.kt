package ch.obermuhlner.kimage.matrix

class FloatMatrix(override val rows: Int, override val columns: Int) : Matrix {
    private val data: FloatArray = FloatArray(columns * rows)

    override fun create(rows: Int, columns: Int): Matrix = FloatMatrix(rows, columns)

    override fun get(index: Int): Double {
        return data[index].toDouble()
    }

    override fun set(index: Int, value: Double) {
        data[index] = value.toFloat()
    }

//    override fun toString(): String {
//        val str = StringBuilder()
//
//        str.append("[")
//        for (y in 0 until rows) {
//            str.append("[")
//            for (x in 0 until columns) {
//                if (x != 0) {
//                    str.append(",")
//                }
//                str.append(this[y, x])
//            }
//            str.append("]")
//            str.appendLine()
//        }
//
//        str.append("]")
//
//        return str.toString()
//    }
}