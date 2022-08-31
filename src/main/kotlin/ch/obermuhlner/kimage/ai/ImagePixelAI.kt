package ch.obermuhlner.kimage.ai

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.math.clamp
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.nd4j.linalg.schedule.MapSchedule
import org.nd4j.linalg.schedule.ScheduleType
import java.io.File
import java.util.*

class ImagePixelAI(
    networkName: String,
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val offsetX: Int,
    private val offsetY: Int,
    hiddenLayerCount: Int = 1,
    hiddenLayerSize: Int = 50,
    private val useImageMirror: Boolean = true,
    private val useImageRotation: Boolean = true,
    private val useAdaptiveMinorityOversamplingFactor: Double = 1.0,
    private val channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue),
    seed: Long = 123
) {
    private val networkFile: File = File("${networkName}_input${inputWidth}x${inputHeight}_layers${hiddenLayerCount}x${hiddenLayerSize}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}.zip")

    private val inputSize = inputWidth * inputHeight * channels.size
    private val outputSize = outputWidth * outputHeight * channels.size
    private val random = Random(seed)

    private val model: MultiLayerNetwork = if (networkFile.exists()) {
        ModelSerializer.restoreMultiLayerNetwork(networkFile)
    } else {
        createNetwork(hiddenLayerCount, hiddenLayerSize)
    }

    private fun createNetwork(hiddenLayerCount: Int, hiddenLayerSize: Int): MultiLayerNetwork {
        val seed = 123L;

        val learningRateSchedule: MutableMap<Int, Double> = HashMap()
        learningRateSchedule[0] = 0.001
        learningRateSchedule[10] = 0.0001
        val mapLearningRateSchedule = MapSchedule(ScheduleType.EPOCH, learningRateSchedule)

        val builder = NeuralNetConfiguration.Builder()
            .seed(seed)
            .l2(0.0005)
            .weightInit(WeightInit.XAVIER)
            .updater(Adam(1e-3))
            //.updater(Adam(0.0005))
            //.updater(Adam(mapLearningRateSchedule))
            //.updater(Nesterovs(0.001, 0.99))
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

        val model = MultiLayerNetwork(builder.build())
        model.init()

        return model
    }

    fun train(inputDirectory: File, outputDirectory: File, suffixes: List<String> = listOf("png"), epochs: Int = 1, imageEpochs: Int = 1, pixelProbability: Double = 1.0) {
        assert(inputDirectory.isDirectory) { "is not a directory: $inputDirectory" }
        assert(outputDirectory.isDirectory) { "is not a directory: $outputDirectory" }
        val inputFiles = inputDirectory.listFiles { _, name -> suffixes.any { suffix -> name.endsWith(".$suffix") } }

        for (i in 0 .. epochs) {
            for (inputFile in inputFiles) {
                if (!inputFile.isFile) {
                    continue
                }
                val outputFile = File(outputDirectory, inputFile.name)
                if (!outputFile.exists() || !outputFile.isFile) {
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
        val dataSets = toDataSets(input, output, pixelProbability, useAdaptiveMinorityOversamplingFactor)
        println("DataSet size ${dataSets.size}")
        dataSets.shuffle()

        val dataSetIterator = ListDataSetIterator(dataSets)

        model.fit(dataSetIterator, epochs)

        println("Score: " + model.score())

        model.save(networkFile, true)
    }

    fun test(input: Image, output: Image) {
        var transformedInput = input
        var transformedOutput = output

        if (useImageMirror) {
            transformedInput = when(random.nextInt(3)) {
                1 -> transformedInput.mirrorX()
                2 -> transformedInput.mirrorY()
                else -> transformedInput
            }
            transformedOutput = when(random.nextInt(3)) {
                1 -> transformedOutput.mirrorX()
                2 -> transformedOutput.mirrorY()
                else -> transformedOutput
            }
        }

        if (useImageRotation) {
            transformedInput = when(random.nextInt(4)) {
                1 -> transformedInput.rotateLeft()
                2 -> transformedInput.rotateRight()
                3 -> transformedInput.rotateRight().rotateRight()
                else -> transformedInput
            }
            transformedOutput = when(random.nextInt(4)) {
                1 -> transformedOutput.rotateLeft()
                2 -> transformedOutput.rotateRight()
                3 -> transformedOutput.rotateRight().rotateRight()
                else -> transformedOutput
            }
        }

        doTest(transformedInput, transformedOutput)
    }

    fun doTest(input: Image, output: Image) {
        val dataSets = toDataSets(input, output)
        val dataSetIterator = ListDataSetIterator(dataSets)

        val evaluation: org.nd4j.evaluation.regression.RegressionEvaluation = model.evaluateRegression(dataSetIterator)
        evaluation.columnNames = channels.map { c -> c.toString() }
        println(evaluation.stats())
    }

    private fun toDataSets(input: Image, output: Image, pixelProbability: Double = 1.0, adaptiveMinorityOverSamplingFactor: Double = 1.0): MutableList<DataSet> {
        val dataSets = mutableListOf<DataSet>()
        if (adaptiveMinorityOverSamplingFactor < 1.0) {
            val dataSetsWithDistance = mutableListOf<Pair<DataSet, Double>>()
            for (y in 0 until input.height) {
                for (x in 0 until input.width) {
                    if (pixelProbability >= 1.0 || random.nextDouble() < pixelProbability) {
                        val inputSub = input.crop(x-offsetX, y-offsetY, inputWidth, inputHeight)
                        val outputSub = output.crop(x, y, outputWidth, outputHeight)
                        val dataSet = toDataSet(inputSub, outputSub)

                        val testOutput = model.output(dataSet.features, false)
                        val dist = testOutput.distance2(dataSet.labels)
                        dataSetsWithDistance.add(Pair(dataSet, dist))
                    }
                }
            }
            dataSetsWithDistance.sortBy { d -> -d.second }
            val minoritySize = clamp((dataSetsWithDistance.size * adaptiveMinorityOverSamplingFactor + 0.5).toInt(), 1, dataSetsWithDistance.size)
            for (i in 0 until minoritySize) {
                dataSets.add(dataSetsWithDistance[i].first)
            }
        } else {
            for (y in 0 until input.height) {
                for (x in 0 until input.width) {
                    if (pixelProbability >= 1.0 || random.nextDouble() < pixelProbability) {
                        val inputSub = input.crop(x-offsetX, y-offsetY, inputWidth, inputHeight)
                        val outputSub = output.crop(x, y, outputWidth, outputHeight)
                        val dataSet = toDataSet(inputSub, outputSub)
                        dataSets.add(dataSet)
                    }
                }
            }
        }
        return dataSets


//        val dataSets = mutableListOf<DataSet>()
//        for (y in 0 until input.height) {
//            for (x in 0 until input.width) {
//                if (pixelProbability >= 1.0 || random.nextDouble() < pixelProbability) {
//                    val inputSub = input.crop(x-offsetX, y-offsetY, inputWidth, inputHeight)
//                    val outputSub = output.crop(x, y, outputWidth, outputHeight)
//                    val dataSet = toDataSet(inputSub, outputSub)
//                    if (adaptiveMinorityOverSampling) {
//                        val testOutput = model.output(dataSet.features, false)
//                        val dist = testOutput.distance2(dataSet.labels)
//                        if (random.nextDouble() < dist) {
//                            dataSets.add(dataSet)
//                        }
//                    } else {
//                        dataSets.add(dataSet)
//                    }
//                }
//            }
//        }
//        return dataSets
    }

    private fun toDataSet(input: Image, output: Image): DataSet {
        val inputArray = DoubleArray(inputSize) { i ->
            val pixelIndex = i / channels.size
            val channelIndex = i % channels.size
            input[channels[channelIndex]][pixelIndex]
        }
        val outputArray = DoubleArray(outputSize) { i ->
            val pixelIndex = i / channels.size
            val channelIndex = i % channels.size
            output[channels[channelIndex]][pixelIndex]
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
        for (y in 0 until output.height step outputHeight) {
            for (x in 0 until output.width step outputWidth) {
                val inputSub = input.crop(x-offsetX, y-offsetY, inputWidth, inputHeight)
                val inputArray = DoubleArray(inputSize) { i ->
                    val pixelIndex = i / channels.size
                    val channelIndex = i % channels.size
                    inputSub[channels[channelIndex]][pixelIndex]
                }
                val inputNdArray: INDArray = Nd4j.create(inputArray)
                val outputNdArray = model.output(inputNdArray)

                for (yOutput in 0 until outputHeight) {
                    for (xOutput in 0 until outputWidth) {
                        for (channelIndex in channels.indices) {
                            val outputIndex = (xOutput + yOutput * outputWidth) * channels.size + channelIndex
                            output[channels[channelIndex]][x+xOutput, y+yOutput] = clamp(outputNdArray.getDouble(outputIndex), 0.0, 1.0)
                        }
                    }
                }
            }
        }

        return output
    }
}