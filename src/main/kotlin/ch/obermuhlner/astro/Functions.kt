package ch.obermuhlner.astro

import java.util.*
import kotlin.math.absoluteValue

fun formatDegreesToHMS(degrees: Double): String {
    val totalSeconds = (degrees / 360 * 24 * 3600)
    val h = (totalSeconds / 3600).toLong()
    val m = (totalSeconds % 3600).toLong() / 60
    val s = totalSeconds % 60

    return String.format(Locale.US, "%02dh%02dm%03.1fs", h, m, s)
}

fun formatDegreesToDMS(degrees: Double): String {
    val totalSeconds = degrees.absoluteValue * 3600
    val d = (totalSeconds / 3600).toLong()
    val m = (totalSeconds % 3600).toLong() / 60
    val s = totalSeconds % 60
    val sign = if (degrees > 0.0) '+' else '-'

    return String.format(Locale.US, "%s%02d°%02d'%03.1f\"", sign, d, m, s)
}