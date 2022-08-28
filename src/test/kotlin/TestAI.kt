import ch.obermuhlner.kimage.ai.ImagePixelAI
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.sobelFilter3
import java.io.File

object TestAI {
    @JvmStatic
    fun main(args: Array<String>) {
        val radius = 2
        val hiddenLayerCount = 0
        val hiddenLayerSize = 10
        val pixelProbability = 1.0

        val description = "radius${radius}_layers${hiddenLayerCount}x${hiddenLayerSize}"
        val ai = ImagePixelAI("gauss", radius, hiddenLayerCount, hiddenLayerSize)

        val trainInput = ImageReader.read(File("images/lena512.png"))
        val trainOutput = trainInput.sobelFilter3()
        ImageWriter.write(trainOutput, File("train_output.png"))

        val testInput = ImageReader.read(File("images/animal.png"))

        val testRefOutput = testInput.sobelFilter3()
        ImageWriter.write(testRefOutput, File("ref_output.png"))

        var epoch = 0
        for (i in 0 until 1000) {
            for (j in 0 until 1) {
                ai.train(trainInput, trainOutput, epochs = 1, pixelProbability = pixelProbability)
                epoch++
            }

            println("Write output images after $epoch epochs")
            val testOutput = ai.run(testInput)
            ImageWriter.write(testOutput, File("ai_output_${description}_epoch${epoch}.png"))

            val testTrainOutput = ai.run(trainInput)
            ImageWriter.write(testTrainOutput, File("ai_train_output_${description}_epoch${epoch}.png"))
        }
    }
}