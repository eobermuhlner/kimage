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
    }

    single {
        var x: Optional<Int> by arguments
        var y: Optional<Int> by arguments
        var x2: Optional<Int> by arguments
        var y2: Optional<Int> by arguments
        var radius: Optional<Int> by arguments
        var radiusX: Optional<Int> by arguments
        var radiusY: Optional<Int> by arguments

        if (!x.isPresent) {
            x = Optional.of(inputImage.width / 2)
        }
        if (!y.isPresent) {
            y = Optional.of(inputImage.height / 2)
        }

        if (radius.isPresent) {
            println("Arguments:")
            println("  x = ${x.get()}")
            println("  y = ${y.get()}")
            println("  radius = ${radius.get()}")
            println()

            inputImage.cropCenter(radius.get(), x.get(), y.get())
        } else {
            if (!radiusX.isPresent) {
                radiusX = Optional.of(inputImage.width / 4)
            }
            if (!radiusY.isPresent) {
                radiusY = Optional.of(inputImage.height / 4)
            }

            println("Arguments:")
            println("  x = ${x.get()}")
            println("  y = ${y.get()}")
            println("  radiusX = ${radiusX.get()}")
            println("  radiusY = ${radiusY.get()}")
            println()

            inputImage.cropCenter(radiusX.get(), radiusY.get(), x.get(), y.get())
        }
    }
}
