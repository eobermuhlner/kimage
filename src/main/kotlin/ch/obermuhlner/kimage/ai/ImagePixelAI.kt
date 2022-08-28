package ch.obermuhlner.kimage.ai

import ch.obermuhlner.kimage.image.Channel
import ch.obermuhlner.kimage.image.Image
import ch.obermuhlner.kimage.image.MatrixImage
import ch.obermuhlner.kimage.image.crop
import ch.obermuhlner.kimage.io.ImageReader
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File
import java.util.*

class ImagePixelAI(
    networkName: String,
    val inputRadius: Int = 3,
    hiddenLayerCount: Int = 1,
    hiddenLayerSize: Int = 50,
    val channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue),
    seed: Long = 123
) {
    private val networkFile: File = File("${networkName}_radius${inputRadius}_layers${hiddenLayerCount}x${hiddenLayerSize}.zip")

    private var model: MultiLayerNetwork
    private val inputImageDiameter = (inputRadius*2 + 1)
    private val inputSize = inputImageDiameter * inputImageDiameter * channels.size
    private val outputSize = channels.size
    private val random = Random(seed)

    init {
        if (networkFile.exists()) {
            model = ModelSerializer.restoreMultiLayerNetwork(networkFile)
        } else {
            model = createNetwork(hiddenLayerCount, hiddenLayerSize)
        }
    }

    private fun createNetwork(hiddenLayerCount: Int, hiddenLayerSize: Int): MultiLayerNetwork {
        val seed = 123L;

        val builder = NeuralNetConfiguration.Builder()
            .seed(seed)
            .l2(0.0005)
            .weightInit(WeightInit.XAVIER)
            .updater(Adam(5e-4))
            //.updater(Nesterovs(0.01, 0.9))
            .list()
            .layer(
                DenseLayer.Builder()
                    .activation(Activation.RELU)
                    .nIn(inputSize)
                    .nOut(hiddenLayerSize)
                    .build()
            )

        for (i in 0 until hiddenLayerCount) {
            builder.layer(
                DenseLayer.Builder()
                    .activation(Activation.RELU)
                    .nIn(hiddenLayerSize)
                    .nOut(hiddenLayerSize)
                    .build()
            )
        }

        builder.layer(
                OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(hiddenLayerSize)
                    .nOut(outputSize)
                    .build()
            )

        model = MultiLayerNetwork(builder.build())
        model.init()

        return model
    }

    fun train(inputDirectory: File, outputDirectory: File, suffixes: List<String> = listOf("png"), epochs: Int = 1, imageEpochs: Int = 1, pixelProbability: Double = 1.0) {
        assert(inputDirectory.isDirectory) { "is not a directory: $inputDirectory" }
        assert(outputDirectory.isDirectory) { "is not a directory: $outputDirectory" }
        val inputFiles = inputDirectory.listFiles { _, name -> suffixes.any { suffix -> name.endsWith(".$suffix") } }

        for (i in 0 .. epochs) {
            for (inputFile in inputFiles) {
                val outputFile = File(outputDirectory, inputFile.name)
                if (!outputFile.exists()) {
                    continue
                }

                val inputImage = ImageReader.read(inputFile)
                val outputImage = ImageReader.read(outputFile)
                if (inputImage.width != outputImage.width || inputImage.height != outputImage.height) {
                    continue
                }

                train(inputImage, outputImage, imageEpochs, pixelProbability)
            }
        }
    }

    fun train(input: Image, output: Image, epochs: Int = 10, pixelProbability: Double = 1.0) {
        val dataSets = mutableListOf<DataSet>()
        for (y in 0 until input.height) {
            for (x in 0 until input.width) {
                if (pixelProbability >= 1.0 || random.nextDouble() < pixelProbability) {
                    val inputSub = input.crop(x-inputRadius, y-inputRadius, inputImageDiameter, inputImageDiameter)
                    val outputSub = output.crop(x, y, 1, 1)
                    val dataSet = toDataSet(inputSub, outputSub)
                    dataSets.add(dataSet)
                }
            }
        }
        dataSets.shuffle()

        model.setListeners(
            ScoreIterationListener(1))

        val dataSetIterator = ListDataSetIterator(dataSets)
        model.fit(dataSetIterator, epochs)

        println("Score: " + model.score())

        model.save(networkFile, true)
    }

    private fun toDataSet(input: Image, output: Image): DataSet {
        val inputArray = DoubleArray(inputSize) { i ->
            val pixelIndex = i / channels.size
            val channelIndex = i % channels.size
            input[channels[channelIndex]][pixelIndex]
        }
        val outputArray = DoubleArray(outputSize) { i ->
            output[channels[i]][0]
        }

        //val inputShape = intArrayOf(inputSize)
        //val outputShape = intArrayOf(outputSize)
        val inputNdArray: INDArray = Nd4j.create(inputArray)
        val outputNdArray: INDArray = Nd4j.create(outputArray)

        return DataSet(inputNdArray, outputNdArray)
    }

    fun run(input: Image): Image {
        val inputShape = intArrayOf(inputSize)

        val output = MatrixImage(input.width, input.height, channels)
        for (y in 0 until output.height) {
            for (x in 0 until output.width) {
                val inputSub = input.crop(x-inputRadius, y-inputRadius, inputImageDiameter, inputImageDiameter)
                val inputArray = DoubleArray(inputSize) { i ->
                    val pixelIndex = i / channels.size
                    val channelIndex = i % channels.size
                    inputSub[channels[channelIndex]][pixelIndex]
                }
                val inputNdArray: INDArray = Nd4j.create(inputArray)
                val outputNdArray = model.output(inputNdArray)

                for (i in channels.indices) {
                    output[channels[i]][x, y] = outputNdArray.getDouble(i)
                }
            }
        }

        return output
    }
}