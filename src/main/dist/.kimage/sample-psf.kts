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
            enabledWhen = Reference("psf").isEqual("sample")
        }
        optionalInt("sampleY") {
            hint = Hint.ImageY
            enabledWhen = Reference("psf").isEqual("sample")
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
        m = m.medianFilter(medianRadius)
        val minValue = m.min()
        val maxValue = m.max()
        m = (m elementMinus minValue) / (maxValue - minValue)

        m.onEach { x, y, value ->
            val dx = (x - radius).toDouble()
            val dy = (y - radius).toDouble()
            val r = sqrt(dx*dx + dy*dy) / radius
            value * (1.0 - smootherstep(smoothRadius, 0.9, r))
        }

        MatrixImage(radius*2+1, radius*2+1,
            Channel.Red to m,
            Channel.Green to m,
            Channel.Blue to m)
    }
}
