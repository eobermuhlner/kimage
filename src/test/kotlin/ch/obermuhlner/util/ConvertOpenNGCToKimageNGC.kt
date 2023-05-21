package ch.obermuhlner.util

import java.io.File

// https://github.com/mattiaverga/OpenNGC
object ConvertOpenNGCToKimageNGC {
    fun parse(file: File) {
        file.readLines().forEach { line ->
            val fields = line.split(';')
            val name = fields[0]
            val type = fields[1]
            if (name != "Name" && type != "NonEx" && type != "Dup") {
                val ra = fields[2]
                val dec = fields[3]
                val majAx = fields[5]
                val minAx = fields[6]
                val posAng = fields[7]
                val vMag = fields[9]
                val messier = fields[23]
                val common = fields[28]

                println("$name;$type;${raToDegrees(ra)};${decToDegrees(dec)};$vMag;$messier;$common;$majAx;$minAx;$posAng")
            }
        }
    }

    fun raToHours(ra: String): Double {
        val parts = ra.split(":").map { it.toDouble() }
        val hours = parts[0]
        val minutes = parts[1]
        val seconds = parts[2]

        return hours + (minutes / 60.0) + (seconds / 3600.0)
    }

    fun raToDegrees(ra: String): Double {
        val parts = ra.split(":").map { it.toDouble() }
        val hours = parts[0]
        val minutes = parts[1]
        val seconds = parts[2]
        return hours * 15 + minutes * 0.25 + seconds * 0.00416667
    }

    fun decToDegrees(dec: String): Double {
        val parts = dec.split(":").map { it.toDouble() }
        val degrees = parts[0]
        val minutes = parts[1]
        val seconds = parts[2]
        return degrees + (minutes / 60) + (seconds / 3600)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        ConvertOpenNGCToKimageNGC.parse(File("C:/Temp/NGC.csv"))
    }
}