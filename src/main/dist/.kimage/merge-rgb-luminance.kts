import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.huge.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.math.*
import ch.obermuhlner.kimage.matrix.*

import java.io.*
import kotlin.math.*

kimage(0.1) {
    name = "merge-rgb-luminance"
    title = "Merge rgb and luminance images"
    description = """
                Merge red, green, blue channels with a luminance channels.
                
                The red, green, blue channels can come from a single image or three separate images.
                """
    arguments {
        string("images") {
            description = """
                        Specifies the layers for merging.
                        - rgb+l : first image is rgb, second image is luminance
                        - rgb+gray : first image is rgb, second image is gray
                        - r+g+b+l : four images red, green, blue, luminance
                        - r+g+b+gray : four images red, green, blue, gray
                    """
            allowed = listOf("rgb+l", "rgb+gray", "r+g+b+l", "r+g+b+gray")
            default = "rgb+l"
        }
        double("redFactor") {
            default = 1.0
        }
        double("greenFactor") {
            default = 1.0
        }
        double("blueFactor") {
            default = 1.0
        }
        double("hueFactor") {
            default = 1.0
        }
        double("saturationFactor") {
            default = 1.0
        }
        double("brightnessFactor") {
            default = 1.0
        }
    }

    multi {
        val images: String by arguments
        val redFactor: Double by arguments
        val greenFactor: Double by arguments
        val blueFactor: Double by arguments
        val hueFactor: Double by arguments
        val saturationFactor: Double by arguments
        val brightnessFactor: Double by arguments

        var inputFileIndex = 0

        val rgbImage = when (images) {
            "rgb+l", "rgb+gray" -> {
                println("Loading rgb image ${inputFiles[inputFileIndex]}")
                val originalRgbImage = ImageReader.read(inputFiles[inputFileIndex++])

                MatrixImage(originalRgbImage.width, originalRgbImage.height,
                    Channel.Red to originalRgbImage[Channel.Red] * redFactor,
                    Channel.Green to originalRgbImage[Channel.Green] * greenFactor,
                    Channel.Blue to originalRgbImage[Channel.Blue] * blueFactor)
            }
            "r+g+b+l", "r+g+b+gray" -> {
                println("Loading red image ${inputFiles[inputFileIndex]}")
                val redImage = ImageReader.read(inputFiles[inputFileIndex++])
                println()

                println("Loading green image ${inputFiles[inputFileIndex]}")
                val greenImage = ImageReader.read(inputFiles[inputFileIndex++])
                println()

                println("Loading red image ${inputFiles[inputFileIndex]}")
                val blueImage = ImageReader.read(inputFiles[inputFileIndex++])
                println()

                MatrixImage(redImage.width, redImage.height,
                    Channel.Red to redImage[Channel.Red] * redFactor,
                    Channel.Green to greenImage[Channel.Green] * greenFactor,
                    Channel.Blue to blueImage[Channel.Blue] * blueFactor)
            }
            else -> throw IllegalArgumentException("Unknown layers: $images")
        }

        println("Loading luminance image ${inputFiles[inputFileIndex]}")
        val luminanceImage = ImageReader.read(inputFiles[inputFileIndex++])

        val luminanceChannel = when (images) {
            "rgb+l", "r+g+b+l" -> {
                luminanceImage[Channel.Luminance]
            }
            "rgb+gray", "r+g+b+gray" -> {
                luminanceImage[Channel.Gray]
            }
            else -> throw IllegalArgumentException("Unknown layers: $images")
        }

        val hue = rgbImage[Channel.Hue] * hueFactor
        val saturation = rgbImage[Channel.Saturation] * saturationFactor
        val brightness = luminanceChannel * brightnessFactor

        val hsbImage = MatrixImage(rgbImage.width, rgbImage.height,
            Channel.Hue to hue,
            Channel.Saturation to saturation,
            Channel.Brightness to brightness)

        MatrixImage(rgbImage.width, rgbImage.height,
            Channel.Red to hsbImage[Channel.Red],
            Channel.Green to hsbImage[Channel.Green],
            Channel.Blue to hsbImage[Channel.Blue])
    }
}
