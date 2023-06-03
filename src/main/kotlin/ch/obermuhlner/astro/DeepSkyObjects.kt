package ch.obermuhlner.astro

import ch.obermuhlner.kimage.javafx.KImageApplication
import kotlin.jvm.*

object DeepSkyObjects {
    data class NGC(val name: String, val type: String, val ra: Double, val dec: Double, val mag: Double?, val messier: Int?, val common: String, val majAx: Double?, val minAx: Double?, val posAngle: Double?) {
        val messierOrName: String get() = if (messier != null) "M${messier}" else name

        val typeEnglish: String get() = when(type) {
            "*" -> "Star"
            "**" -> "Double Star"
            "OCl" -> "Open Cluster"
            "GCl" -> "Globular Cluster"
            "Cl+N" -> "Star cluster + Nebula"
            "G" -> "Galaxy"
            "GPair" -> "Galaxy Pair"
            "GTrpl" -> "Galaxy Triplet"
            "GGroup" -> "Galaxy Group"
            "PN" -> "Planetary Nebula"
            "HII" -> "HII Ionized region"
            "DrkN" -> "Dark Nebula"
            "EmN" -> "Emission Nebula"
            "Neb" -> "Nebula"
            "RfN" -> "Reflection Nebula"
            "SNR" -> "Supernova remnant"
            "Nova" -> "Nova"
            "NonEx" -> "Nonexistent"
            "Dup" -> "Duplicate"
            "Other" -> "Other"
            else -> "Unknown"
        }
    }

    fun all(): List<NGC> {
        val inputStream = KImageApplication::class.java.getResourceAsStream("/NGC.csv")
        val ngcLines = inputStream.bufferedReader().use { it.readLines() }

        val ngcs = mutableListOf<NGC>()
        for (ngcLine in ngcLines) {
            val fields = ngcLine.split(';')
            val name = fields[0]
            val type = fields[1]
            val ra = fields[2].toDouble()
            val dec = fields[3].toDouble()
            val mag = if (fields[4].isEmpty()) null else fields[4].toDouble()
            val messier = if (fields[5].isEmpty()) null else fields[5].toInt()
            val common = fields[6]
            val majAx = if (fields[7].isEmpty()) null else fields[7].toDouble()/60
            val minAx = if (fields[8].isEmpty()) null else fields[8].toDouble()/60
            val posAngle = if (fields[9].isEmpty()) null else fields[9].toDouble()
            ngcs += NGC(name, type, ra, dec, mag, messier, common, majAx, minAx, posAngle)
        }
        return ngcs
    }
}