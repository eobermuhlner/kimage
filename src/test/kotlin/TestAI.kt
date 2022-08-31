import ch.obermuhlner.kimage.ai.ImagePixelAI
import ch.obermuhlner.kimage.image.crop
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.sobelFilter3
import org.nd4j.evaluation.regression.RegressionEvaluation
import java.io.File

object TestAI {
    @JvmStatic
    fun main(args: Array<String>) {
        testSobel()
        //testDebayer()
    }

    fun testDebayer() {
        val inputWidth = 6
        val inputHeight = 6
        val outputWidth = 2
        val outputHeight = 2
        val offsetX = 2
        val offsetY = 2
        val hiddenLayerCount = 1
        val hiddenLayerSize = 10
        val pixelProbability = 1.0

        val description = "input${inputWidth}x${inputHeight}_layers${hiddenLayerCount}x${hiddenLayerSize}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}"

        val ai = ImagePixelAI(
            "debayer",
            inputWidth,
            inputHeight,
            outputWidth,
            outputHeight,
            offsetX,
            offsetY,
            hiddenLayerCount,
            hiddenLayerSize)

        val trainInput = ImageReader.read(File("images/misc/bayer/bayer_from_debayer_superpixel/IMG_2426.tif"))
        val trainOutput = ImageReader.read(File("images/misc/bayer/debayer_superpixel/IMG_2426.tif"))

        val testInput = ImageReader.read(File("images/misc/bayer/orig_raw/convert-raw_IMG_2425.tif")).crop(2800, 2000, 1000, 1000)

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

//        ai.train(
//            File("images/misc/bayer/debayer_superpixel"),
//            File("images/misc/bayer/bayer_from_debayer_superpixel"),
//            listOf("tif"),
//            1,
//            1,
//        1.0)
//
//        val testOutput = ai.run(testInput)
//        ImageWriter.write(testOutput, File("test_output_$description.tif"))
    }

    fun testSobel() {
        val inputWidth = 3
        val inputHeight = 3
        val outputWidth = 1
        val outputHeight = 1
        val offsetX = 1
        val offsetY = 1
        val hiddenLayerCount = 0
        val hiddenLayerSize = 50
        val pixelProbability = 1.0

        val description = "input${inputWidth}x${inputHeight}_layers${hiddenLayerCount}x${hiddenLayerSize}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}"
        val ai = ImagePixelAI(
            "sobel3",
            inputWidth,
            inputHeight,
            outputWidth,
            outputHeight,
            offsetX,
            offsetY,
            hiddenLayerCount,
            hiddenLayerSize)

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

            ai.test(testInput, testRefOutput)

            println("Write output images after $epoch epochs")
            val testOutput = ai.run(testInput)
            ImageWriter.write(testOutput, File("ai_output_${description}_epoch${epoch}.png"))

            val testTrainOutput = ai.run(trainInput)
            ImageWriter.write(testTrainOutput, File("ai_train_output_${description}_epoch${epoch}.png"))
        }
    }
}