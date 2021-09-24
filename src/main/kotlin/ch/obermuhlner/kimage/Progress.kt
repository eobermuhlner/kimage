package ch.obermuhlner.kimage

interface Progress {
    fun addTotal(totalStepCount: Int, message: String = "")
    fun step(stepCount: Int = 1, message: String = "")
}