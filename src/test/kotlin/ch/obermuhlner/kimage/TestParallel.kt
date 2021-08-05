package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.matrix.Matrix
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.system.measureTimeMillis

object TestParallel {
    @JvmStatic
    fun main(args: Array<String>) {
        //exampleParallel()
        exampleConvoluteParallel()
    }

    fun exampleParallel() = runBlocking {
        launch {
            delay(1000L)
            println("World")
        }
        println("Hello")
    }

    private fun exampleConvoluteParallel() {
        val matrixSize = 100
        val kernelSize = 100

        val m = Matrix.matrixOf(matrixSize, matrixSize)
        val kernel = Matrix.matrixOf(kernelSize, kernelSize)

        // warmup
        m.convolute(kernel)

        val millisSequential = measureTimeMillis {
            val m2 = m.convolute(kernel)
        }
        println("sequential: $millisSequential ms")

        // warmup
        m.convoluteParallel(kernel)

        val millisParallel = measureTimeMillis {
            val m2 = m.convoluteParallel(kernel)
        }
        println("parallel : $millisParallel ms")

        // warmup
        m.convoluteParallelBlocks(kernel, 1)
        m.convoluteParallelBlocks(kernel, 2)

        for (yStep in listOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000)) {
            val millisParallelBlocks = measureTimeMillis {
                val m2 = m.convoluteParallelBlocks(kernel, yStep)
            }
            println("parallelBlocks $yStep : $millisParallelBlocks ms")
        }
    }

    fun Matrix.convoluteParallel(kernel: Matrix): Matrix = runBlocking {
        val m = create()
        for (y in 0 until height) {
            launch(Dispatchers.Default) {
                for (x in 0 until width) {
                    //ensureActive()
                    var value = 0.0
                    for (kernelY in 0 until kernel.height) {
                        for (kernelX in 0 until kernel.width) {
                            val pixel =
                                this@convoluteParallel[x - kernel.width / 2 + kernelX, y - kernel.height / 2 + kernelY]
                            value += pixel * kernel[kernelX, kernelY]
                        }
                    }
                    m[x, y] = value
                }
            }
        }
        m
    }

    fun Matrix.convoluteParallelBlocks(kernel: Matrix, yStep: Int = 1): Matrix = runBlocking {
        val m = create()
        for (yBlock in 0 until height step yStep) {
            launch(Dispatchers.Default) {
                for (y in yBlock until min(yBlock+yStep, height)) {
                    //ensureActive()
                    for (x in 0 until width) {
                        var value = 0.0
                        for (kernelY in 0 until kernel.height) {
                            for (kernelX in 0 until kernel.width) {
                                val pixel =
                                    this@convoluteParallelBlocks[x - kernel.width / 2 + kernelX, y - kernel.height / 2 + kernelY]
                                value += pixel * kernel[kernelX, kernelY]
                            }
                        }
                        m[x, y] = value
                    }
                }
            }
        }
        m
    }

}