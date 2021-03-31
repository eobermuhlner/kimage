package ch.obermuhlner.astro.gradient.align

import ch.obermuhlner.kimage.image.Image

interface ImageAligner {

    fun align(base: Image, image: Image, centerX: Int = base.width/2, centerY: Int = base.height/2, maxOffset: Int = 200): Alignment

    data class Alignment(val x: Int, val y: Int, val error: Double)
}