import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import java.io.*
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "crop"
    title = "Crop an image"
    description = """
                Crop an image according the specified arguments.
                """
    arguments {
        optionalInt("x") {
            hint = Hint.ImageX
        }
        optionalInt("y") {
            hint = Hint.ImageY
        }
        optionalInt("x2") {
            hint = Hint.ImageX
        }
        optionalInt("y2") {
            hint = Hint.ImageY
        }
        optionalInt("width") {
        }
        optionalInt("height") {
        }
        optionalInt("radius") {
        }
    }

    single {
        var x: Optional<Int> by arguments
        var y: Optional<Int> by arguments
        var x2: Optional<Int> by arguments
        var y2: Optional<Int> by arguments
        var width: Optional<Int> by arguments
        var height: Optional<Int> by arguments
        var radius: Optional<Int> by arguments

        if (radius.isPresent) {
            if (!x.isPresent) {
                x = Optional.of(inputImage.width / 2)
            }
            if (!y.isPresent) {
                y = Optional.of(inputImage.width / 2)
            }

            println("Arguments:")
            println("  x = ${x.get()}")
            println("  y = ${y.get()}")
            println("  radius = ${radius.get()}")
            println()

            inputImage.cropCenter(radius.get(), x.get(), y.get())
        } else if (x2.isPresent && y2.isPresent){
            if (!x.isPresent) {
                x = Optional.of(0)
            }
            if (!y.isPresent) {
                y = Optional.of(0)
            }

            width = Optional.of(x2.get() - x.get())
            height = Optional.of(y2.get() - y.get())

            println("Arguments:")
            println("  x = ${x.get()}")
            println("  y = ${y.get()}")
            println("  x2 = ${x2.get()}")
            println("  y2 = ${y2.get()}")
            println("  width = ${width.get()}")
            println("  height = ${height.get()}")
            println()

            inputImage.crop(x.get(), y.get(), width.get(), height.get())
        } else {
            if (!x.isPresent) {
                x = Optional.of(0)
            }
            if (!y.isPresent) {
                y = Optional.of(0)
            }
            if (!width.isPresent) {
                width = Optional.of(inputImage.width / 2)
            }
            if (!height.isPresent) {
                height = Optional.of(inputImage.height / 2)
            }

            println("Arguments:")
            println("  x = ${x.get()}")
            println("  y = ${y.get()}")
            println("  width = ${width.get()}")
            println("  height = ${height.get()}")
            println()

            inputImage.crop(x.get(), y.get(), width.get(), height.get())
        }
    }
}
