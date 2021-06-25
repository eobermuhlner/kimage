package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.Scaling
import org.junit.Test

class MatrixFunctionsTest {

    @Test
    fun testScaleTo() {
        val m = Matrix.matrixOf(2, 2, 0.1, 0.2, 0.3, 0.4)
        val m2 = m.scaleTo(4, 4, Scaling.Nearest)
        println(m2)
    }
}