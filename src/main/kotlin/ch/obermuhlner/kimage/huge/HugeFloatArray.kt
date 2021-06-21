package ch.obermuhlner.kimage.huge

import java.io.File
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class HugeFloatArray(vararg val dimensions: Int) {

    private val maxBufferSize = Integer.MAX_VALUE / 4

    private val elementSize = 4 // float has 4 bytes

    private val floatBuffers: Array<FloatBuffer>
    private val bufferSize: Long

    init {
        val n = dimensions.map(Int::toLong).reduce(Long::times)

        val bufferCount = (n / maxBufferSize).toInt() + 1 // at least one buffer
        bufferSize = n / bufferCount + 1 // prevent rounding down and missing 1 element
        val bufferByteSize = bufferSize * elementSize
        floatBuffers = Array(bufferCount) {
            val file = File.createTempFile("HugeFloatArray_${it}_", ".mem")
            println("bufferFile : $file")
            file.deleteOnExit()

            val channel: FileChannel = Files.newByteChannel(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE) as FileChannel
            val buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, bufferByteSize)

            buffer.asFloatBuffer()
        }
    }

    operator fun get(index: Long): Float {
        val bufferIndex = (index / bufferSize).toInt()
        val bufferModuloIndex = (index % bufferSize).toInt()
        return floatBuffers[bufferIndex][bufferModuloIndex]
    }
    operator fun set(index: Long, value: Float) {
        val bufferIndex = (index / bufferSize).toInt()
        val bufferModuloIndex = (index % bufferSize).toInt()
        floatBuffers[bufferIndex].put(bufferModuloIndex, value)
    }

    operator fun get(index0: Int, index1: Int): Float {
        require(dimensions.size >= 2)
        val index = index0.toLong() + index1.toLong() * dimensions[0]
        return get(index)
    }
    operator fun set(index0: Int, index1: Int, value: Float) {
        require(dimensions.size >= 2)
        val index = index0.toLong() + index1.toLong() * dimensions[0]
        set(index, value)
    }

    operator fun get(index0: Int, index1: Int, index2: Int): Float {
        require(dimensions.size >= 3)
        val index = index0.toLong() + index1.toLong() * dimensions[0] + index2.toLong() * (dimensions[0]*dimensions[1])
        return get(index)
    }
    operator fun set(index0: Int, index1: Int, index2: Int, value: Float) {
        require(dimensions.size >= 3)
        val index = index0.toLong() + index1.toLong()* dimensions[0] + index2.toLong() * (dimensions[0]*dimensions[1])
        set(index, value)
    }

    operator fun get(index0: Int, index1: Int, index2: Int, index3: Int): Float {
        require(dimensions.size >= 4)
        val index = index0.toLong() + index1.toLong() * dimensions[0] + index2.toLong() * (dimensions[0]*dimensions[1]) + index3.toLong() * (dimensions[0]*dimensions[1]*dimensions[2])
        return get(index)
    }
    operator fun set(index0: Int, index1: Int, index2: Int, index3: Int, value: Float) {
        require(dimensions.size >= 4)
        val index = index0.toLong() + index1.toLong() * dimensions[0] + index2.toLong() * (dimensions[0]*dimensions[1]) + index3.toLong() * (dimensions[0]*dimensions[1]*dimensions[2])
        set(index, value)
    }

}