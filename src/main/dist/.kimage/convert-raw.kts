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
            description = """
               The `dcraw` executable.
               
               If the executable is not in the `PATH` then the absolute path is required to run it.
            """
            default = "dcraw"
        }
        string("rotate") {
            description = """
               The angle to rotate the image.
               - `auto` will use the information in the image rotate. 
               
              This corresponds to the `-t` option in the `dcraw` command line tool.
            """
            allowed = listOf("0", "90", "180", "270", "auto")
            default = "auto"
        }
        boolean("aspectRatio") {
            description = """
               
              This corresponds to the `-j` option in the `dcraw` command line tool.
            """
            default = true
        }
        string("whitebalance") {
            description = """
              The whitebalance setting used to adjust the colors.
              
              - `camera` will use the whitebalance settings measured by the camera (if available)
              - `image` will calculate the whitebalance settings from the image
              - `local` will calculate the whitebalance settings from a local area of the image
              - `custom` will use the provided custom multipliers
              - `fixed` will use fixed default white balance multipliers.
              
              The `camera` whitebalance corresponds to the `-w` option in the `dcraw` command line tool.
              The `image` whitebalance corresponds to the `-a` option in the `dcraw` command line tool.
              The `custom` whitebalance corresponds to the `-r` option in the `dcraw` command line tool.
              The `fixed` whitebalance corresponds to the `-W` option in the `dcraw` command line tool.
            """
            allowed = listOf("camera", "image", "local", "custom", "fixed")
            default = "camera"
        }
        optionalInt("localX") {
            hint = Hint.ImageX
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        optionalInt("localY") {
            hint = Hint.ImageY
            enabledWhen = Reference("whitebalance").isEqual("local")
        }
        optionalInt("localRadius") {
            enabledWhen = Reference("whitebalance").isEqual("local")
            default = 10
        }
        optionalList("multipliers") {
            description = """
              The four multipliers used for `custom` whitebalance mode.
              
              Corresponds to the `-r` option in the `dcraw` command line tool.
              """
            min = 4
            max = 4
            enabledWhen = Reference("whitebalance").isEqual("custom")
            double {
            }
        }
        optionalInt("darkness") {
            description = """
              The darkness level.
              
              Corresponds to the `-k` option in the `dcraw` command line tool.
              """
            min = 0
        }
        optionalInt("saturation") {
            description = """
              The saturation level.
              
              Corresponds to the `-S` option in the `dcraw` command line tool.
              """
            min = 0
        }
        string("colorspace") {
            description = """
                The colorspace to be used for the output image.
                """
            allowed = listOf("raw", "sRGB", "AdobeRGB", "WideGamutRGB", "KodakProPhotoRGB", "XYZ", "ACES", "embed")
            default = "sRGB"
        }
        string("interpolation") {
            description = """
                The demosaicing interpolation method to use.
                
                - `bilinear`: Bilinear interpolation between neighboring pixels of the same color.
                
                  Corresponds to the `-q 0` option in the `dcraw` command line tool.
                - `VNG`: Variable Number Gradients
                
                  Corresponds to the `-q 1` option in the `dcraw` command line tool.
                - `PPG`: Patterned Pixel Grouping
                
                  Corresponds to the `-q 2` option in the `dcraw` command line tool.
                - `AHD`: Adaptive Homogeneity Directed
                
                  Corresponds to the `-q 3` option in the `dcraw` command line tool.
                - `none`: No interpolation, with automatic scaling to fill the value range.
                
                  Corresponds to the `-d` option in the `dcraw` command line tool.
                - `none-unscaled`: No interpolation, no scaling.
                
                  Corresponds to the `-D` option in the `dcraw` command line tool.
                - `none-uncropped`: No interpolation, no scaling, no cropping.
                
                  Corresponds to the `-E` option in the `dcraw` command line tool.
                """
            allowed = listOf("bilinear", "VNG", "PPG", "AHD", "none", "none-unscaled", "none-uncropped")
            default = "AHD"
        }
        int("medianPasses") {
            description = """
                The number of 3x3 median passes to post-process the output image.
                
                Corresponds to the `-m` option in the `dcraw` command line tool.
                """
            default = 0
        }
        string("bits") {
            description = """
                The number of bits used to store a single value in the image.                
                
                The 16 bit mode corresponds to the `-6` option in the `dcraw` command line tool.
                """
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
        localX: Optional<Int>,
        localY: Optional<Int>,
        localRadius: Optional<Int>,
        multipliers: Optional<List<Double>>,
        darkness: Optional<Int>,
        saturation: Optional<Int>,
        colorspace: String,
        interpolation: String,
        medianPasses: Int,
        bits: String,
        brightness: Double,
        file: File,
        outputDirectory: File
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
            "local" -> {
                command.add("-A")
                command.add((localX.get() - localRadius.get()).toString())
                command.add((localY.get() - localRadius.get()).toString())
                command.add((localRadius.get() * 2 + 1).toString())
                command.add((localRadius.get() * 2 + 1).toString())

            }
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
        if (darkness.isPresent) {
            command.add("-k");
            command.add(darkness.get().toString());
        }
        if (saturation.isPresent) {
            command.add("-S");
            command.add(saturation.get().toString());
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
            "VNG" -> {
                command.add("-q")
                command.add("1")
            }
            "PPG" -> {
                command.add("-q")
                command.add("2")
            }
            "AHD" -> {
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

        command.add("-O")
        command.add(file.prefixName(outputDirectory, "${name}_").replaceExtension("tif").path)

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
        val localX: Optional<Int> by arguments
        val localY: Optional<Int> by arguments
        val localRadius: Optional<Int> by arguments
        val multipliers: Optional<List<Double>> by arguments
        val darkness: Optional<Int> by arguments
        val saturation: Optional<Int> by arguments
        val colorspace: String by arguments
        val interpolation: String by arguments
        val medianPasses: Int by arguments
        val bits: String by arguments
        val brightness: Double by arguments

        for (inputFile in inputFiles) {
            println("Converting $inputFile")
            dcraw(dcraw, aspectRatio, rotate, whitebalance, localX, localY, localRadius, multipliers, darkness, saturation, colorspace, interpolation, medianPasses, bits, brightness, inputFile, outputDirectory)
            println()
        }

        null
    }
}
