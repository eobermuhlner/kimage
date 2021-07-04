import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import java.io.*
import java.util.*
import kotlin.math.*

kimage(0.1) {
    name = "calibrate"
    description = """
                Calibrates bias/dark/flat/darkflat/light images.
                """
    arguments {
        optionalImage("bias") {
        }
        optionalImage("dark") {
        }
        optionalImage("flat") {
        }
        optionalImage("darkflat") {
        }
    }

    multi {
        println("Calibrate")
        println()

        var bias: Optional<Image> by arguments
        var dark: Optional<Image> by arguments
        var flat: Optional<Image> by arguments
        var darkflat: Optional<Image> by arguments
        val applyBiasOnCalibration = false

        println("Arguments:")
        println("  bias = $bias")
        println("  dark = $dark")
        println("  flat = $flat")
        println("  darkflat = $darkflat")
        println()

        if (applyBiasOnCalibration && bias.isPresent) {
            if (dark.isPresent) {
                dark = Optional.of(dark.get() - bias.get())
            }
            if (darkflat.isPresent) {
                darkflat = Optional.of(darkflat.get() - bias.get())
            }
            if (flat.isPresent) {
                flat = Optional.of(flat.get() - bias.get())
            }
        }

        if (darkflat.isPresent) {
            flat = Optional.of(flat.get() - darkflat.get())
        }

        for (inputFile in inputFiles) {
            println("Loading $inputFile")
            var light = ImageReader.read(inputFile)

            if (bias.isPresent) {
                light = light - bias.get()
            }
            if (dark.isPresent) {
                light = light - dark.get()
            }
            if (flat.isPresent) {
                light = light / flat.get()
            }

            ImageWriter.write(light, inputFile.prefixName("calibrated_"))
        }

        null
    }
}
