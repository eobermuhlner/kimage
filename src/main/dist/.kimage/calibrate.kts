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
                Calibrates images using bias/dark/flat/darkflat images.
                
                The different calibration files are optional, specify only the calibration image you have.
                
                ### Creating Calibration Images
                
                Create about 20-50 images of each calibration image type.
                
                - `bias` images 
                  - camera with lens cap on
                  - same ISO as for real pictures
                  - fastest exposure time
                - `flat` images
                  - camera against homogeneous light source (e.g. t-shirt over lens against sky)
                  - same objective + focus as for real pictures
                  - same aperture as for real pictures
                  - set exposure time so that histogram shows most pixels at ~50%
                - `darkflat` images
                  - camera with lens cap on
                  - same objective + focus as for real pictures
                  - same aperture as for real pictures
                  - same exposure time as for `flat` images
                - `dark` images
                  - camera with lens cap on
                  - same objective + focus as for real pictures
                  - same aperture as for real pictures
                  - same exposure time as for real pictures
                  - same temperature as for real pictures
                  - (usually take the dark pictures immediately after taking the real pictures)
                
                Stack the `bias` images with:
                
                    kimage stack --arg method=median bias*.TIF
                The output will be your master `bias` image - rename it accordingly.
                
                Calibrate all other images with the `bias` images and stack them.
                
                For example the `flat` images:
                
                    kimage calibrate --arg bias=master_bias.TIF flat*.TIF
                    kimage stack --arg method=median calibrate_flat*.TIF
                
                Do this for the `flat`, `darkflat` and `dark` images.
                The outputs will be your master `flat`, `darkflat` and `dark` images - rename them accordingly.
                
                Calibrate the real images:
                
                    kimage calibrate --arg bias=master_bias.TIF --arg flat=master_flat.TIF --arg darkflat=master_darkflat.TIF --arg dark=master_dark.TIF light*.TIF
                    
                See: http://deepskystacker.free.fr/english/theory.htm
                """
    arguments {
        optionalImage("bias") {
            description = """
                The `bias` master calibration image.
                
                This argument is optional.
                If no `bias` image is specified it will not be used in the calibration process.
                """
        }
        optionalImage("dark") {
            description = """
                The `dark` master calibration image.
                
                This argument is optional.
                If no `dark` image is specified it will not be used in the calibration process.
                """
        }
        optionalImage("flat") {
            description = """
                The `flat` master calibration image.
                
                This argument is optional.
                If no `flat` image is specified it will not be used in the calibration process.
                """
        }
        optionalImage("darkflat") {
            description = """
                The `darkflat` master calibration image.
                
                This argument is optional.
                If no `flat` image is specified it will not be used in the calibration process.
                """
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
