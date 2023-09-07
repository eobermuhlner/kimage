import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "crop-center"
    title = "Crop a centered image"
    description = """
                Crop a centered image according the specified arguments.
                """
    arguments {
        optionalInt("x") {
            description = """
                The center x coordinate to crop.
                """
            hint = Hint.ImageX
        }
        optionalInt("y") {
            description = """
                The center y coordinate to crop.
                """
            hint = Hint.ImageY
        }
        optionalInt("radius") {
            description = """
                The radius of the square (both x and y axis) to crop.
                """
        }
        optionalInt("radiusX") {
            description = """
                The radius along the x axis to crop.
                """
        }
        optionalInt("radiusY") {
            description = """
                The radius along the y axis to crop.
                """
        }
        boolean("avoidBorders") {
            description = """
                Avoid clipping borders by moving the cropped area.
                """
            default = false
        }
    }

    single {
        var x: Optional<Int> by arguments
        var y: Optional<Int> by arguments
        var radius: Optional<Int> by arguments
        var radiusX: Optional<Int> by arguments
        var radiusY: Optional<Int> by arguments
        var avoidBorders: Boolean by arguments

        if (!x.isPresent) {
            x = Optional.of(inputImage.width / 2)
        }
        if (!y.isPresent) {
            y = Optional.of(inputImage.height / 2)
        }

        if (!radiusX.isPresent) {
            radiusX = Optional.of(inputImage.width / 4)
        }
        if (!radiusY.isPresent) {
            radiusY = Optional.of(inputImage.height / 4)
        }

        var xx = x.get()
        var yy = y.get()
        if (avoidBorders) {
            if (radius.isPresent) {
                xx = if (xx - radius.get() < 0) radius.get() else xx
                yy = if (yy - radius.get() < 0) radius.get() else yy
                xx = if (xx + radius.get() > inputImage.width) inputImage.width - radius.get() else xx
                yy = if (yy + radius.get() > inputImage.height) inputImage.height - radius.get() else yy
            } else {
                xx = if (xx - radiusX.get() < 0) radiusX.get() else xx
                yy = if (yy - radiusY.get() < 0) radiusY.get() else yy
                xx = if (xx + radiusX.get() > inputImage.width) inputImage.width - radiusX.get() else xx
                yy = if (yy + radiusY.get() > inputImage.height) inputImage.height - radiusY.get() else yy
            }
        }

        if (radius.isPresent) {
            println("Arguments:")
            println("  x = $xx")
            println("  y = $yy")
            println("  radius = ${radius.get()}")
            println()

            inputImage.cropCenter(radius.get(), xx, yy)
        } else {

            println("Arguments:")
            println("  x = $xx")
            println("  y = $yy")
            println("  radiusX = ${radiusX.get()}")
            println("  radiusY = ${radiusY.get()}")
            println()

            inputImage.cropCenter(radiusX.get(), radiusY.get(), xx, yy)
        }
    }
}
