import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.matrix.*
import java.io.*
import java.util.*
import kotlin.math.*
import jwave.Transform
import jwave.transforms.*
import jwave.transforms.wavelets.haar.*
import jwave.transforms.wavelets.daubechies.*
import jwave.transforms.wavelets.symlets.*
import jwave.transforms.wavelets.coiflet.*
import jwave.transforms.wavelets.legendre.*
import jwave.transforms.wavelets.biorthogonal.*
import jwave.transforms.wavelets.other.*

kimage(0.1) {
    name = "wavelet-denoise"
    title = "Denoise image using wavelet transformation"
    description = """
                Denoise image using wavelet transformation.
                """
    arguments {
        string("wavelet") {
            allowed = listOf("Haar", "HaarOrthogonal", "Daubechies", "Symlet", "Coiflet", "Legendre", "BiOrthogonal", "CDF", "DiscreteMayer")
            default = "Daubechies"
        }
        string("daubechiesVanishingMoments") {
            allowed = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20")
            default = "20"
            enabledWhen = Reference("wavelet").isEqual("Daubechies")
        }
        string("symletOrder") {
            allowed = listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20")
            default = "20"
            enabledWhen = Reference("wavelet").isEqual("Symlet")
        }
        string("coifletOrder") {
            allowed = listOf("1", "2", "3", "4", "5")
            default = "5"
            enabledWhen = Reference("wavelet").isEqual("Coiflet")
        }
        string("biOrthogonalOrder") {
            allowed = listOf("1.1", "1.3", "1.5", "2.2", "2.4", "2.6", "2.8", "3.1", "3.3", "3.5", "3.7", "3.9", "4.4", "5.5", "6.8")
            default = "3.3"
            enabledWhen = Reference("wavelet").isEqual("BiOrthogonal")
        }
        string("cdfOrder") {
            allowed = listOf("5.3", "9.7")
            default = "9.7"
            enabledWhen = Reference("wavelet").isEqual("CDF")
        }
        string("legendreOrder") {
            allowed = listOf("1", "2", "3")
            default = "3"
            enabledWhen = Reference("wavelet").isEqual("Legendre")
        }
        double("threshold") {
            default = 0.1
        }
        string("thresholdFunction") {
            allowed = listOf("soft", "hard", "fade-sigmoid", "fade-power")
            default = "fade-sigmoid"
        }
        double("fadePower") {
            default = 0.5
            enabledWhen = Reference("thresholdFunction").isEqual("fade-power")
        }
    }

    single {
        val wavelet: String by arguments
        val daubechiesVanishingMoments: String by arguments
        val symletOrder: String by arguments
        val coifletOrder: String by arguments
        val biOrthogonalOrder: String by arguments
        val cdfOrder: String by arguments
        val legendreOrder: String by arguments
        val threshold: Double by arguments
        val thresholdFunction: String by arguments
        val fadePower: Double by arguments

        val waveletFunction = when(wavelet) {
            "Haar" -> Haar1()
            "HaarOrthogonal" -> Haar1Orthogonal()
            "DiscreteMayer" -> DiscreteMayer()
            "Daubechies" -> when (daubechiesVanishingMoments) {
                "2" -> Daubechies2()
                "3" -> Daubechies3()
                "4" -> Daubechies4()
                "5" -> Daubechies5()
                "6" -> Daubechies6()
                "7" -> Daubechies7()
                "8" -> Daubechies8()
                "9" -> Daubechies9()
                "10" -> Daubechies10()
                "11" -> Daubechies11()
                "12" -> Daubechies12()
                "13" -> Daubechies13()
                "14" -> Daubechies14()
                "15" -> Daubechies15()
                "16" -> Daubechies16()
                "17" -> Daubechies17()
                "18" -> Daubechies18()
                "19" -> Daubechies19()
                "20" -> Daubechies20()
                else -> throw IllegalArgumentException("Unsupported vanishing moments for Daubechies wavelet: $daubechiesVanishingMoments")
            }
            "Symlet" -> when (symletOrder) {
                "2" -> Symlet2()
                "3" -> Symlet3()
                "4" -> Symlet4()
                "5" -> Symlet5()
                "6" -> Symlet6()
                "7" -> Symlet7()
                "8" -> Symlet8()
                "9" -> Symlet9()
                "10" -> Symlet10()
                "11" -> Symlet11()
                "12" -> Symlet12()
                "13" -> Symlet13()
                "14" -> Symlet14()
                "15" -> Symlet15()
                "16" -> Symlet16()
                "17" -> Symlet17()
                "18" -> Symlet18()
                "19" -> Symlet19()
                "20" -> Symlet20()
                else -> throw IllegalArgumentException("Unsupported order for Symlet wavelet: $symletOrder")
            }
            "Coiflet" -> when (coifletOrder) {
                "1" -> Coiflet1()
                "2" -> Coiflet2()
                "3" -> Coiflet3()
                "4" -> Coiflet4()
                "5" -> Coiflet5()
                else -> throw IllegalArgumentException("Unsupported vanishing moments for Coiflet wavelet: $coifletOrder")
            }
            "BiOrthogonal" -> when (biOrthogonalOrder) {
                "1.1" -> BiOrthogonal11()
                "1.3" -> BiOrthogonal13()
                "1.5" -> BiOrthogonal15()
                "2.2" -> BiOrthogonal22()
                "2.4" -> BiOrthogonal24()
                "2.6" -> BiOrthogonal26()
                "2.8" -> BiOrthogonal28()
                "3.1" -> BiOrthogonal31()
                "3.3" -> BiOrthogonal33()
                "3.5" -> BiOrthogonal35()
                "3.7" -> BiOrthogonal37()
                "3.9" -> BiOrthogonal39()
                "4.4" -> BiOrthogonal44()
                "5.5" -> BiOrthogonal55()
                "6.8" -> BiOrthogonal68()
                else -> throw IllegalArgumentException("Unsupported order for BiOrthogonal wavelet: $biOrthogonalOrder")
            }
            "CDF" -> when (cdfOrder) {
                "5.3" -> CDF53()
                "9.7" -> CDF97()
                else -> throw IllegalArgumentException("Unsupported vanishing moments for CDF wavelet: $cdfOrder")
            }
            "Legendre" -> when (legendreOrder) {
                "1" -> Coiflet1()
                "2" -> Coiflet2()
                "3" -> Coiflet3()
                else -> throw IllegalArgumentException("Unsupported order for Legendre wavelet: $legendreOrder")
            }
            else -> throw IllegalArgumentException("Unknown wavelet: $wavelet")
        }

        val transform = Transform(FastWaveletTransform(waveletFunction))

        fun nextPower2(value: Int): Int {
            var v = value
            v--
            v = v or (v shr 1)
            v = v or (v shr 2)
            v = v or (v shr 4)
            v = v or (v shr 8)
            v = v or (v shr 16)
            v++
            return v
        }

        val image = inputImage.crop(0, 0, nextPower2(inputImage.width), nextPower2(inputImage.height))
        MatrixImage(inputImage.width, inputImage.height, inputImage.channels) { channel, _, _ ->
            val matrix = image[channel]

            val waveletCoefficients = transform.forward(matrix.toDoubleArray())
            for (i in waveletCoefficients.indices) {
                for (j in waveletCoefficients[i].indices) {
                    val v = abs(waveletCoefficients[i][j])
                    when (thresholdFunction) {
                        "soft" -> {
                            if (v < threshold) {
                                waveletCoefficients[i][j] = 0.0
                            } else {
                                waveletCoefficients[i][j] = sign(waveletCoefficients[i][j]) * (v - threshold)
                            }
                        }
                        "hard" -> {
                            if (v < threshold) {
                                waveletCoefficients[i][j] = 0.0
                            }
                        }
                        "fade-power" -> {
                            if (v < threshold) {
                                val x = v/threshold
                                val y = x.pow(fadePower)
                                waveletCoefficients[i][j] *= y
                            }
                        }
                        "fade-sigmoid" -> {
                            if (v < threshold) {
                                val x = v/threshold
                                val y = 1.0 / (1.0 + exp(-x))
                                waveletCoefficients[i][j] *= y
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown threshold function: $thresholdFunction")
                    }
                }
            }
            val reverse = transform.reverse(waveletCoefficients)
            Matrix.matrixOf(inputImage.width, inputImage.height) { x, y ->
                reverse[y][x]
            }
        }
    }
}
