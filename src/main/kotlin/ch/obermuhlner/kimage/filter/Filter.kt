package ch.obermuhlner.kimage.filter

interface Filter<T> {
    fun filter(source: T): T
}