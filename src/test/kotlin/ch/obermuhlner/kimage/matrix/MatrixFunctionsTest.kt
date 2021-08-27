package ch.obermuhlner.kimage.matrix

import ch.obermuhlner.kimage.Scaling
import org.junit.Assert.assertEquals
import org.junit.Test

class MatrixFunctionsTest {

    @Test
    fun testScaleToNearest() {
        val matrix = Matrix.matrixOf(2, 2,
            0.1, 0.2,
            0.3, 0.4)
        val scaled = matrix.scaleTo(4, 4, 0.0, 0.0, Scaling.Nearest)

        val expected = Matrix.matrixOf(4, 4,
            0.1, 0.1, 0.2, 0.2,
            0.1, 0.1, 0.2, 0.2,
            0.3, 0.3, 0.4, 0.4,
            0.3, 0.3, 0.4, 0.4)
        assertEquals(expected, scaled)
    }
}