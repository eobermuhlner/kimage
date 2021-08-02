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
    name = "deconvolute"
    title = "Deconvolute an image"
    description = """
                Deconvolute an image.
                """
    arguments {
        string("method") {
            allowed = listOf("lucy")
            default = "lucy"
        }
        string("psf") {
            allowed = listOf("gauss3x3", "gauss5x5", "gauss", "moffat", "sample")
            default = "gauss3x3"
        }
        optionalInt("sampleX") {
            hint = Hint.ImageX
            enabledWhen = Reference("psf").isEqual("sample")
        }
        optionalInt("sampleY") {
            hint = Hint.ImageY
            enabledWhen = Reference("psf").isEqual("sample")
        }
        double("background") {
            enabledWhen = Reference("psf").isEqual("gauss", "moffat")
            default = 0.0
        }
        double("amplitude") {
            enabledWhen = Reference("psf").isEqual("gauss", "moffat")
            default = 1.0
        }
        double("beta") {
            enabledWhen = Reference("psf").isEqual("moffat")
            default = 1.0
        }
        double("sigmaX") {
            enabledWhen = Reference("psf").isEqual("gauss", "moffat")
            default = 2.0
        }
        double("sigmaY") {
            enabledWhen = Reference("psf").isEqual("gauss", "moffat")
            default = 2.0
        }
        int("radius") {
            enabledWhen = Reference("psf").isEqual("sample", "gauss", "moffat")
            default = 3
        }
        int("iterations") {
            default = 100
        }
    }

    single {
        val method: String by arguments
        val psf: String by arguments
        val sampleX: Optional<Int> by arguments
        val sampleY: Optional<Int> by arguments
        val background: Double by arguments
        val amplitude: Double by arguments
        val beta: Double by arguments
        val sigmaX: Double by arguments
        val sigmaY: Double by arguments
        val radius: Int by arguments
        val iterations: Int by arguments

        fun gauss(
            x: Double,
            y: Double,
            background: Double = 0.0,
            amplitude: Double = 1.0 - background,
            sigmaX: Double = 2.0,
            sigmaY: Double = 2.0,
            x0: Double = 0.0,
            y0: Double = 0.0
        ): Double {
            val dx = x - x0
            val dy = y - y0
            return background + amplitude * exp(- ((dx*dx)/2.0/sigmaX/sigmaX + (dy*dy)/2.0/sigmaY/sigmaY))
        }

        fun moffat(
            x: Double,
            y: Double,
            background: Double = 0.0,
            amplitude: Double = 1.0 - background,
            beta: Double = 1.0,
            sigmaX: Double = 2.0,
            sigmaY: Double = 2.0,
            x0: Double = 0.0,
            y0: Double = 0.0
        ): Double {
            val dx = x - x0
            val dy = y - y0
            return background + amplitude / (1.0 + ((dx*dx)/sigmaX/sigmaX + (dy*dy)/sigmaY/sigmaY)).pow(beta)
        }

        fun Matrix.deconvolute(psfKernel: Matrix, steps: Int): Matrix {
            val psfTransposed = psfKernel.transpose()

            var approx = this
            for (i in 0 until steps) {
                val approx2 = approx elementTimes ((this elementDiv (approx.convolute(psfKernel))).convolute(psfTransposed))
                approx = approx2
            }
            return approx
        }

        val outputMatrices: MutableMap<Channel, Matrix> = mutableMapOf()
        for (channel in inputImage.channels) {
            println("Processing channel: $channel")
            val psfKernel = when (psf) {
                "gauss3x3" -> KernelFilter.GaussianBlur3
                "gauss5x5" -> KernelFilter.GaussianBlur5
                "sample" -> {
                    val m = inputImage[channel].cropCenter(radius, sampleX.get(), sampleY.get())
                    m / m.sum()
                }
                "gauss" -> {
                    DoubleMatrix(radius*2+1, radius*2+1) { y, x ->
                        gauss((x - radius).toDouble(), (y - radius).toDouble(), background, amplitude, sigmaX, sigmaY)
                    }
                }
                "moffat" -> {
                    DoubleMatrix(radius*2+1, radius*2+1) { y, x ->
                        moffat((x - radius).toDouble(), (y - radius).toDouble(), background, amplitude, beta, sigmaX, sigmaY)
                    }
                }
                else -> throw IllegalArgumentException("Unknown psf: $psf")
            }

            outputMatrices[channel] = inputImage[channel].deconvolute(psfKernel, iterations)
        }
        println()

        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ -> outputMatrices[channel]!! }
    }
}
