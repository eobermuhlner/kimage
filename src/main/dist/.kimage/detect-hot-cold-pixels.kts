import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.util.*
import kotlin.math.*
import java.lang.Math.toRadians

kimage(0.1) {
    name = "detect-hot-cold-pixels"
    title = "Detect hot and cold pixels."
    description = """
                Detect hot and cold pixels.
                The input image should be a dark or uniformly grey image. 
                """
    arguments {
        boolean("overviewOnly") {
            default = true
        }
        double("stddevFactor") {
            default = 100.0
        }
        boolean("reportValue0") {
            default = true
        }
        boolean("reportValue1") {
            default = true
        }
    }
    single {
        val overviewOnly: Boolean by arguments
        val stddevFactor: Double by arguments
        val reportValue0: Boolean by arguments
        val reportValue1: Boolean by arguments

        val channel = Channel.Red
        val median = inputImage[channel].median()
        val stddev = inputImage[channel].stddev()
        println("median = $median")
        println("stddev = $stddev")

        var countExtremeLow = 0
        var countExtremeHigh = 0
        var countLow = 0
        var countHigh = 0
        for (y in 0 until inputImage.height) {
            for (x in 0 until inputImage.width) {
                val value = inputImage[channel][x, y]
                if (reportValue0 && value <= 0.0) {
                    countExtremeLow++
                } else if (reportValue1 && value >= 1.0) {
                    countExtremeHigh++
                } else if (value < median - stddevFactor*stddev) {
                    countLow++
                } else if (value > median + stddevFactor*stddev) {
                    countHigh++
                }
            }
        }
        println("Overview:")
        if (reportValue0) {
            println("Extreme Low: $countExtremeLow")
        }
        if (reportValue1) {
            println("Extreme High: $countExtremeHigh")
        }
        println("Low: $countLow")
        println("High: $countHigh")

        if (!overviewOnly) {
            for (y in 0 until inputImage.height) {
                for (x in 0 until inputImage.width) {
                    val value = inputImage[channel][x, y]
                    if (reportValue0 && value <= 0.0) {
                        println("ExtremeLow $x, $y $value")
                    } else if (reportValue1 && value >= 1.0) {
                        println("ExtremeHigh $x, $y $value")
                    } else if (value < median - stddevFactor*stddev) {
                        println("Low $x, $y $value")
                    } else if (value > median + stddevFactor*stddev) {
                        println("High $x, $y $value")
                    }
                }
            }
        }
    }
}
