package ch.obermuhlner.kimage

import ch.obermuhlner.kimage.filter.Shape
import ch.obermuhlner.kimage.image.Channel.*
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.javafx.KImageApplication
import ch.obermuhlner.kimage.javafx.KImageApplication.Companion.interactive
import ch.obermuhlner.kotlin.javafx.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import java.io.File

object KImage {
    @JvmStatic
    fun main(args: Array<String>) {
        example()
    }

    private fun example() {
        val originalImage = ImageReader.readMatrixImage(File("images/animal.png"))
        //val originalImage = ImageReader.readMatrixImage(File("images/orion_small_compress0.png"))
        //val originalImage = ImageReader.readMatrixImage(File("images/orion_32bit.tif"))

        interactive {
            setCurrentImage(originalImage, "Original")

            val radiusProperty = SimpleIntegerProperty(3)
            form {
                children += vbox {
                    children += label("Median Radius:")
                    children += textfield(radiusProperty) {}
                }
            }

            filter ("Median") {
                median(radiusProperty.get(), Shape.Cross).median(radiusProperty.get(), Shape.DiagonalCross)
            }
        }

        interactive {
            val radiusProperty = SimpleIntegerProperty(3)
            form {
                children += vbox {
                    children += label("Blur Radius:")
                    children += textfield(radiusProperty) {}
                }
            }
            filter("Blur") {
                gaussianBlur(radiusProperty.get())
            }
        }

        interactive {
            val removalFactorProperty = SimpleDoubleProperty(1.0)
            form {
                children += vbox {
                    children += label("Removal Factor:")
                    children += textfield(removalFactorProperty, KImageApplication.PERCENT_FORMAT) {}
                }
            }
            filterArea("Subtract") { x, y, w, h ->
                val croppedOriginalImage = originalImage.croppedImage(x, y, w, h)
                MatrixImage(
                    this.width,
                    this.height,
                    Red to croppedOriginalImage[Red] - this[Red] * removalFactorProperty.get(),
                    Green to croppedOriginalImage[Green] - this[Green] * removalFactorProperty.get(),
                    Blue to croppedOriginalImage[Blue] - this[Blue] * removalFactorProperty.get())
            }
        }

        interactive {
            ImageWriter.write(currentImage!!, File("images/background_removed.png"))
        }
    }
}
