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
    name = "sample-psf"
    title = "Create PSF from image sample"
    description = """
                Create a PSF (Point Spread Function) from an image sample.
                The output image can be used in deconvolution as PSF.
                """
    arguments {
        optionalInt("sampleX") {
            hint = Hint.ImageX
        }
        optionalInt("sampleY") {
            hint = Hint.ImageY
        }
        int("medianRadius") {
            default = 1
        }
        double("smoothRadius") {
            default = 0.5
        }
        int("radius") {
            default = 10
        }
    }

    single {
        val sampleX: Optional<Int> by arguments
        val sampleY: Optional<Int> by arguments
        val medianRadius: Int by arguments
        val smoothRadius: Double by arguments
        val radius: Int by arguments

        var m = inputImage[Channel.Gray].cropCenter(radius, sampleX.get(), sampleY.get())

        if (verboseMode) {
            println("cropped =")
            println(m.contentToString(true))
        }
        m = m.medianFilter(medianRadius)
        if (verboseMode) {
            println("median filtered =")
            println(m.contentToString(true))
        }

        val minValue = m.min()
        if (verboseMode) {
            println("min = $minValue")
        }
        m -= minValue
        if (verboseMode) {
            println("subtracted minValue =")
            println(m.contentToString(true))
        }

        val maxValue = m.max()
        if (verboseMode) {
            println("max = $maxValue")
        }
        m = m / m.max()
        if (verboseMode) {
            println("divided maxValue =")
            println(m.contentToString(true))
        }

        m.onEach { x, y, value ->
            val dx = (x - radius).toDouble()
            val dy = (y - radius).toDouble()
            val r = sqrt(dx*dx + dy*dy) / radius
            value * (1.0 - smootherstep(smoothRadius, 1.0, r))
        }
        if (verboseMode) {
            println("smoothstepped =")
            println(m.contentToString(true))
        }

        MatrixImage(radius*2+1, radius*2+1,
            Channel.Red to m,
            Channel.Green to m,
            Channel.Blue to m)
    }
}
