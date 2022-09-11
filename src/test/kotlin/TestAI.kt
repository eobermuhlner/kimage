import ch.obermuhlner.kimage.ai.*
import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.matrix.mirrorX
import ch.obermuhlner.kimage.matrix.mirrorY
import ch.obermuhlner.kimage.sobelFilter3
import org.nd4j.evaluation.regression.RegressionEvaluation
import java.io.File

object TestAI {
    @JvmStatic
    fun main(args: Array<String>) {
        testDenoise()
        //testSobel()
        //testDebayer()
    }

    /**
     * Training data for "Denoise" was prepared as follows:
     *
     * - 10 original light frames (no dark, no bias, no flat) (discard bad images - distorted, satellites, ...)
     * - in DeepSkyStacker register and stack  with the option "Save registered images" enabled
     * - in Kimage stack 10 registered (=aligned) images (default values: sigma clip median) (compare all images with stacked)
     * - crop all 20 images (removing 200px at the border)
     * - move registered images into "input" folder
     * - copy stacked image 10 times into "output" folder and rename the copies the same as files in the input folder
     * - in Kimage color-stretch-linear low=0.0 high=99.9 the 10 input files
     * - in Kimage color-stretch-linear low=0.0 high=99.9 the 10 output files
     * - in Kimage color-stretch-linear low=0.0 high=100 the 10 input files
     * - in Kimage color-stretch-linear low=0.0 high=10 the 10 output files
     */
    fun testDenoise() {
        val inputWidth = 5
        val inputHeight = 5
        val outputWidth = 1
        val outputHeight = 1
        val offsetX = 2
        val offsetY = 2
        val hiddenLayerSizes = listOf(20) // listOf(50, 9)

        val description = "input${inputWidth}x${inputHeight}_layers${hiddenLayerSizes}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}"

        val ai = ImagePixelAI(
            "denoise",
            inputWidth,
            inputHeight,
            outputWidth,
            outputHeight,
            offsetX,
            offsetY,
            hiddenLayerSizes,
            imagePreProcessors = listOf(
                ImageMirrorPreProcessor(),
                ImageRotate90PreProcessor(),
            ),
            dataSetImagePreProcessors = listOf(
                RandomDataSetImagePreProcessor(0.001),
                GenericDataSetImagesPreProcessor(ImageNormalizePreProcessor()),
                //SampleBiasPreProcessor(),
            )
        )

        val inputDir = File("images/ai/denoise/train/input")
        val outputDir = File("images/ai/denoise/train/output")

        val testInput = ImageReader.read(File("images/ai/denoise/test/input/crop_1/crop_crop_Light_M2_120.0s_Bin1_ISO200_20220823-231355_0001.reg.tif"))
        val testRefOutput = ImageReader.read(File("images/ai/denoise/test/output/crop_1/crop_crop_Light_M2_120.0s_Bin1_ISO200_20220823-231355_0001.reg.tif"))

        var epoch = 0
        for (i in 0 until 1000) {
            for (j in 0 until 10) {
                ai.train(
                    inputDir,
                    outputDir,
                    epochs = 1,
                    suffixes = listOf("tif"),
                    fileCount = 2
                )
//                ai.train(
//                    testInput,
//                    testRefOutput,
//                    epochs = 1
//                )
                epoch++
            }

            println("Write output images after $epoch epochs")
            val testOutput = ai.run(testInput, true, true)
            ImageWriter.write(testOutput, File("ai_output_${description}_epoch${epoch}.png"))
        }
    }

    fun testDebayer() {
        val inputWidth = 6
        val inputHeight = 6
        val outputWidth = 2
        val outputHeight = 2
        val offsetX = 2
        val offsetY = 2
        val hiddenLayerSizes = listOf(10)
        val pixelProbability = 1.0

        val description = "input${inputWidth}x${inputHeight}_layers${hiddenLayerSizes}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}"

        val ai = ImagePixelAI(
            "debayer",
            inputWidth,
            inputHeight,
            outputWidth,
            outputHeight,
            offsetX,
            offsetY,
            hiddenLayerSizes)

        val trainInput = ImageReader.read(File("images/misc/bayer/bayer_from_debayer_superpixel/IMG_2426.tif"))
        val trainOutput = ImageReader.read(File("images/misc/bayer/debayer_superpixel/IMG_2426.tif"))

        val testInput = ImageReader.read(File("images/misc/bayer/orig_raw/convert-raw_IMG_2425.tif")).crop(2800, 2000, 1000, 1000)

        var epoch = 0
        for (i in 0 until 1000) {
            for (j in 0 until 1) {
                ai.train(trainInput, trainOutput, epochs = 1)
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
        val hiddenLayerSizes = listOf<Int>()
        val pixelProbability = 1.0

        val description = "input${inputWidth}x${inputHeight}_layers${hiddenLayerSizes}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}"
        val ai = ImagePixelAI(
            "sobel3",
            inputWidth,
            inputHeight,
            outputWidth,
            outputHeight,
            offsetX,
            offsetY,
            hiddenLayerSizes,
            imagePreProcessors = listOf(ImageMirrorPreProcessor(), ImageRotate90PreProcessor()))

        val trainInput = ImageReader.read(File("images/lena512.png"))
        val trainOutput = trainInput.sobelFilter3()
        ImageWriter.write(trainOutput, File("train_output.png"))

        val trainInput2 = MatrixImage(trainInput[Channel.Red], trainInput[Channel.Green].mirrorX(), trainInput[Channel.Blue].mirrorY())
        val trainOutput2 = trainInput2.sobelFilter3()

        val testInput = ImageReader.read(File("images/animal.png"))

        val testRefOutput = testInput.sobelFilter3()
        ImageWriter.write(testRefOutput, File("ref_output.png"))

        var epoch = 0
        for (i in 0 until 1000) {
            for (j in 0 until 1) {
                ai.train(trainInput, trainOutput, epochs = 1)
                ai.train(trainInput2, trainOutput2, epochs = 1)
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