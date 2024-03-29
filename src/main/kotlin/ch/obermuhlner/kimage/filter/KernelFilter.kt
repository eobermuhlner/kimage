package ch.obermuhlner.kimage.filter

import ch.obermuhlner.kimage.matrix.Matrix

class KernelFilter(private val kernel: Matrix)
    : MatrixImageFilter({ _, source -> source.convolute(kernel) }) {

    companion object {
        val EdgeDetectionStrong = Matrix.matrixOf(
            3, 3,
            -1.0, -1.0, -1.0,
            -1.0, 8.0, -1.0,
            -1.0, -1.0, -1.0)

        val EdgeDetectionCross = Matrix.matrixOf(
                3, 3,
                0.0, -1.0, 0.0,
                -1.0, 4.0, -1.0,
                0.0, -1.0, 0.0)

        val EdgeDetectionDiagonal = Matrix.matrixOf(
                3, 3,
                -1.0, 0.0, -1.0,
                0.0, 4.0, 0.0,
                -1.0, 0.0, -1.0)

        val Sharpen = Matrix.matrixOf(
                3, 3,
                0.0, -1.0, 0.0,
                -1.0, 5.0, -1.0,
                0.0, -1.0, 0.0)

        val BoxBlur3 = Matrix.matrixOf(
                3, 3,
                1.0, 1.0, 1.0,
                1.0, 1.0, 1.0,
                1.0, 1.0, 1.0) / 9.0

        val GaussianBlur3 = Matrix.matrixOf(
                3, 3,
                1.0, 2.0, 1.0,
                2.0, 4.0, 2.0,
                1.0, 2.0, 1.0) / 16.0

        val GaussianBlur5 = Matrix.matrixOf(
                5, 5,
                1.0, 4.0, 6.0, 4.0, 1.0,
                4.0, 16.0, 24.0, 16.0, 4.0,
                16.0, 24.0, 36.0, 24.0, 16.0,
                4.0, 16.0, 24.0, 16.0, 4.0,
                1.0, 4.0, 6.0, 4.0, 1.0) / 256.0

        val GaussianBlur7 = Matrix.matrixOf(
            7, 7,
            0.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0,
            0.0, 3.0, 13.0, 22.0, 13.0, 3.0, 0.0,
            1.0, 13.0, 59.0, 97.0, 59.0, 13.0, 1.0,
            2.0, 22.0, 97.0, 159.0, 97.0, 22.0, 2.0,
            1.0, 13.0, 59.0, 97.0, 59.0, 13.0, 1.0,
            0.0, 3.0, 13.0, 22.0, 13.0, 3.0, 0.0,
            0.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0,) / 1003.0


        val UnsharpMask = Matrix.matrixOf(
                5, 5,
                1.0, 4.0, 6.0, 4.0, 1.0,
                4.0, 16.0, 24.0, 16.0, 4.0,
                16.0, 24.0, -476.0, 24.0, 16.0,
                4.0, 16.0, 24.0, 16.0, 4.0,
                1.0, 4.0, 6.0, 4.0, 1.0) / -256.0

        val Emboss = Matrix.matrixOf(
                3, 3,
                -2.0, -1.0, 0.0,
                -1.0, 1.0, 1.0,
                0.0, 1.0, 2.0)

        val SobelHorizontal3 = Matrix.matrixOf(3, 3,
            1.0, 0.0, -1.0,
            2.0, 0.0, -2.0,
            1.0, 0.0, -1.0)

        val SobelVertical3 = Matrix.matrixOf(3, 3,
            1.0, 2.0, 1.0,
            0.0, 0.0, 0.0,
            -1.0, -2.0, -1.0)

        val SobelHorizontal5 = Matrix.matrixOf(5, 5,
            2.0, 1.0, 0.0, -1.0, -2.0,
            2.0, 1.0, 0.0, -1.0, -2.0,
            4.0, 2.0, 0.0, -2.0, -4.0,
            2.0, 1.0, 0.0, -1.0, -2.0,
            2.0, 1.0, 0.0, -1.0, -2.0)

        val SobelVertical5 = Matrix.matrixOf(5, 5,
            2.0, 2.0, 4.0, 2.0, 2.0,
            1.0, 1.0, 2.0, 1.0, 1.0,
            0.0, 0.0, 0.0, 0.0, 0.0,
            -1.0, -1.0, -2.0, -1.0, -1.0,
            -2.0, -2.0, -4.0, -2.0, -2.0)

    }
}