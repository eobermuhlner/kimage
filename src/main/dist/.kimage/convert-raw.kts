import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.util.*
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.math.*

kimage(0.1) {
    name = "convert-raw"
    title = "Convert an image from raw format into tiff"
    description = """
                Convert an image from raw format into tiff.
                """
    arguments {
        string("dcraw") {
            default = "dcraw"
        }
        string("rotate") {
            allowed = listOf("0", "90", "180", "270", "auto")
            default = "auto"
        }
        boolean("aspectRatio") {
            default = true
        }
        string("whitebalance") {
            allowed = listOf("camera", "image", "custom", "fixed")
            default = "camera"
        }
        optionalList("multipliers") {
            min = 4
            max = 4
            double {
            }
        }
        string("colorspace") {
            allowed = listOf("raw", "sRGB", "AdobeRGB", "WideGamutRGB", "KodakProPhotoRGB", "XYZ", "ACES", "embed")
            default = "sRGB"
        }
        string("interpolation") {
            allowed = listOf("bilinear", "variable-number-gradients", "patterned-pixel-grouping", "adaptive-homogeneity-directed", "none", "none-unscaled", "none-uncropped")
            default = "adaptive-homogeneity-directed"
        }
        int("medianPasses") {
            default = 0
        }
        string("bits") {
            allowed = listOf("8", "16")
            default = "16"
        }
//        record("gamma") {
//            double("gammaPower") {
//                default = 2.222
//            }
//            double("gammaSlope") {
//                default = 4.5
//            }
//        }
        double("brightness") {
            default = 1.0
        }
    }

    fun dcraw(
        dcraw: String,
        aspectRatio: Boolean,
        rotate: String,
        whitebalance: String,
        multipliers: Optional<List<Double>>,
        colorspace: String,
        interpolation: String,
        medianPasses: Int,
        bits: String,
        brightness: Double,
        file: File
    ) {
        val processBuilder = ProcessBuilder()

        val command = mutableListOf(dcraw, "-T", "-v")
        if (!aspectRatio) {
            command.add("-j")
        }
        when (rotate) {
            "0" -> {
                command.add("-t")
                command.add("0")
            }
            "90" -> {
                command.add("-t")
                command.add("90")
            }
            "180" -> {
                command.add("-t")
                command.add("180")
            }
            "270" -> {
                command.add("-t")
                command.add("270")
            }
            "auto" -> {}
            else -> throw java.lang.IllegalArgumentException("Unknown rotate: $rotate")
        }
        when (whitebalance) {
            "camera" -> command.add("-w")
            "image" -> command.add("-a")
            "custom" -> {
                command.add("-r")
                if (multipliers.isPresent) {
                    command.add(multipliers.get()[0].toString())
                    command.add(multipliers.get()[1].toString())
                    command.add(multipliers.get()[2].toString())
                    command.add(multipliers.get()[3].toString())
                } else {
                    command.add("1")
                    command.add("1")
                    command.add("1")
                    command.add("1")
                }
            }
            "fixed" -> command.add("-W")
            else -> throw java.lang.IllegalArgumentException("Unknown whitebalance: $whitebalance")
        }
        when (colorspace) {
            "raw" -> {
                command.add("-o")
                command.add("0")
            }
            "sRGB" -> {
                command.add("-o")
                command.add("1")
            }
            "AdobeRGB" -> {
                command.add("-o")
                command.add("2")
            }
            "WideGamutRGB" -> {
                command.add("-o")
                command.add("3")
            }
            "KodakProPhotoRGB" -> {
                command.add("-o")
                command.add("4")
            }
            "XYZ" -> {
                command.add("-o")
                command.add("5")
            }
            "ACES" -> {
                command.add("-o")
                command.add("6")
            }
            "embed" -> {
                command.add("-p")
                command.add("embed")
            }
            else -> throw java.lang.IllegalArgumentException("Unknown colorspace: $colorspace")
        }
        when (interpolation) {
            // "bilinear", "variable-number-gradients", "patterned-pixel-grouping", "adaptive-homogeneity-directed", "none", "none-unscaled", "none-uncropped"
            "bilinear" -> {
                command.add("-q")
                command.add("0")
            }
            "variable-number-gradients" -> {
                command.add("-q")
                command.add("1")
            }
            "patterned-pixel-grouping" -> {
                command.add("-q")
                command.add("2")
            }
            "adaptive-homogeneity-directed" -> {
                command.add("-q")
                command.add("3")
            }
            "none" -> command.add("-d")
            "none-unscaled" -> command.add("-D")
            "none-uncropped" -> command.add("-E")
            else -> throw java.lang.IllegalArgumentException("Unknown interpolation: $interpolation")
        }
        if (medianPasses > 0) {
            command.add("-m")
            command.add(medianPasses.toString())
        }
        when (bits) {
            "16" -> command.add("-6")
            else -> {}
        }
        command.add("-b")
        command.add(brightness.toString())
        command.add(file.path)

        println("Command: $command")

        processBuilder.command(command)
        //processBuilder.directory(file.parentFile)

        val process = processBuilder.start()

        Executors.newSingleThreadExecutor().submit(StreamGobbler(process.errorStream, System.out::println))
        val exitCode = process.waitFor()
        println("Exit code: $exitCode")
    }

    multi {
        val dcraw: String by arguments
        val aspectRatio: Boolean by arguments
        val rotate: String by arguments
        val whitebalance: String by arguments
        val multipliers: Optional<List<Double>> by arguments
        val colorspace: String by arguments
        val interpolation: String by arguments
        val medianPasses: Int by arguments
        val bits: String by arguments
        val brightness: Double by arguments

        for (inputFile in inputFiles) {
            println("Converting $inputFile")
            dcraw(dcraw, aspectRatio, rotate, whitebalance, multipliers, colorspace, interpolation, medianPasses, bits, brightness, inputFile)
            println()
        }

        null
    }
}
