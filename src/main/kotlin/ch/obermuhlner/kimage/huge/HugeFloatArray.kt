package ch.obermuhlner.kimage.huge

import java.io.File
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class HugeFloatArray(val file: File, vararg val dimensions: Int) {

    private val floatBuffer: FloatBuffer

    init {
        val n = dimensions.map(Int::toLong).reduce(Long::times)
        val bufferSize = n * 4
        val channel: FileChannel = Files.newByteChannel(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE) as FileChannel
        val buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize)
        floatBuffer = buffer.asFloatBuffer()
    }

    operator fun get(index: Int): Float {
        return floatBuffer[index]
    }
    operator fun set(index: Int, value: Float) {
        floatBuffer.put(index, value)
    }

    operator fun get(index0: Int, index1: Int): Float {
        require(dimensions.size >= 2)
        val index = index0 + index1 * dimensions[0]
        return get(index)
    }
    operator fun set(index0: Int, index1: Int, value: Float) {
        require(dimensions.size >= 2)
        val index = index0 + index1 * dimensions[0]
        set(index, value)
    }

    operator fun get(index0: Int, index1: Int, index2: Int): Float {
        require(dimensions.size >= 3)
        val index = index0 + index1 * dimensions[0] + index2 * (dimensions[0]*dimensions[1])
        return get(index)
    }
    operator fun set(index0: Int, index1: Int, index2: Int, value: Float) {
        require(dimensions.size >= 3)
        val index = index0 + index1 * dimensions[0] + index2 * (dimensions[0]*dimensions[1])
        set(index, value)
    }

    operator fun get(index0: Int, index1: Int, index2: Int, index3: Int): Float {
        require(dimensions.size >= 4)
        val index = index0 + index1 * dimensions[0] + index2 * (dimensions[0]*dimensions[1]) + index3 * (dimensions[0]*dimensions[1]*dimensions[2])
        return get(index)
    }
    operator fun set(index0: Int, index1: Int, index2: Int, index3: Int, value: Float) {
        require(dimensions.size >= 4)
        val index = index0 + index1 * dimensions[0] + index2 * (dimensions[0]*dimensions[1]) + index3 * (dimensions[0]*dimensions[1]*dimensions[2])
        set(index, value)
    }

}