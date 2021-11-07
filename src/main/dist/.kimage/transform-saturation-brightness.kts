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

kimage(0.1) {
    name = "transform-saturation-brightness"
    title = "Transform the saturation and brightness of an image"
    description = """
                Transform the saturation and brightness of an image.
                """
    arguments {
        string("saturationFunction") {
            allowed = listOf("linear", "power", "exaggerate")
            default = "linear"
        }
        double("saturationFactor") {
            default = 1.0
        }
        string("brightnessFunction") {
            allowed = listOf("linear", "power", "exaggerate")
            default = "linear"
        }
        double("brightnessFactor") {
            default = 1.0
        }
    }

    single {
        val saturationFunction: String by arguments
        val saturationFactor: Double by arguments
        val brightnessFunction: String by arguments
        val brightnessFactor: Double by arguments

        val hue = inputImage[Channel.Hue]
        val saturation = inputImage[Channel.Saturation]
        val brightness = inputImage[Channel.Brightness]

        val changedSaturation = when(saturationFunction) {
            "linear" -> saturation * saturationFactor
            "power" -> saturation.copy().onEach { v -> v.pow(1.0/saturationFactor) }
            "exaggerate" -> saturation.copy().onEach { v -> exaggerate(v * saturationFactor) }
            else -> throw IllegalArgumentException("Unknown function: $saturationFunction")
        }
        val changedBrightness = when(brightnessFunction) {
            "linear" -> brightness * brightnessFactor
            "power" -> brightness.copy().onEach { v -> v.pow(1.0/brightnessFactor) }
            "exaggerate" -> brightness.copy().onEach { v -> exaggerate(v * brightnessFactor) }
            else -> throw IllegalArgumentException("Unknown function: $brightnessFunction")
        }

        val hsbImage = MatrixImage(inputImage.width, inputImage.height,
            Channel.Hue to hue,
            Channel.Saturation to changedSaturation,
            Channel.Brightness to changedBrightness)

        MatrixImage(inputImage.width, inputImage.height,
            Channel.Red to hsbImage[Channel.Red],
            Channel.Green to hsbImage[Channel.Green],
            Channel.Blue to hsbImage[Channel.Blue])
    }
}
