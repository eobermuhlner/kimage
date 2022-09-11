import ch.obermuhlner.kimage.*
import ch.obermuhlner.kimage.align.*
import ch.obermuhlner.kimage.filter.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.*
import ch.obermuhlner.kimage.ai.*
import java.io.*
import java.util.*
import kotlin.math.*


kimage(0.1) {
    name = "run-ai"
    title = "Run AI on an image"
    description = """
                Process an image using an AI.
                """
    arguments {
        string("ai") {
            description = """
                The name of the AI to run.
                """
            default = "denoise"
        }
        int("inputWidth") {
            description = """
                The input width in pixels.
                """
            default = 5
        }
        int("inputHeight") {
            description = """
                The input height in pixels.
                """
            default = 5
        }
        int("outputWidth") {
            description = """
                The output width in pixels.
                """
            default = 1
        }
        int("outputHeight") {
            description = """
                The output height in pixels.
                """
            default = 1
        }
        int("offsetX") {
            description = """
                The output X offset in pixels.
                """
            default = 2
        }
        int("offsetY") {
            description = """
                The output Y offset in pixels.
                """
            default = 2
        }
        boolean("normalizeMin") {
            description = """
                Normalize the min values.
                """
            default = true
        }
        boolean("normalizeMax") {
            description = """
                Normalize the max values.
                """
            default = true
        }
    }

    single {
        val ai: String by arguments

        val inputWidth: Int by arguments
        val inputHeight: Int by arguments
        val outputWidth: Int by arguments
        val outputHeight: Int by arguments
        val offsetX: Int by arguments
        val offsetY: Int by arguments

        val normalizeMin: Boolean by arguments
        val normalizeMax: Boolean by arguments

        val model = ImagePixelAI(
            ai,
            inputWidth,
            inputHeight,
            outputWidth,
            outputHeight,
            offsetX,
            offsetY
        )

        model.run(inputImage, normalizeMin, normalizeMax)
    }
}
