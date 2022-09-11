package ch.obermuhlner.kimage.ai

import ch.obermuhlner.kimage.image.*
import ch.obermuhlner.kimage.io.ImageReader
import ch.obermuhlner.kimage.io.ImageWriter
import ch.obermuhlner.kimage.math.clamp
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.buffer.DataType
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
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
    hiddenLayerSizes: List<Int> = listOf(),
    private val channels: List<Channel> = listOf(Channel.Red, Channel.Green, Channel.Blue),
    private val imagePreProcessors: List<ImagePreProcessor> = listOf(),
    private val dataSetImagePreProcessors: List<DataSetImagePreProcessor> = listOf(),
    private val dataSetPreProcessors: List<DataSetPreProcessor> = listOf(),
    seed: Long = 123
) {
    private val networkFile: File = File("${networkName}_input${inputWidth}x${inputHeight}_layers${hiddenLayerSizes}_output${outputWidth}x${outputHeight}_offset${offsetX}x${offsetY}.zip")

    private val inputSize = inputWidth * inputHeight * channels.size
    private val outputSize = outputWidth * outputHeight * channels.size
    private val random = Random(seed)

    private val model: MultiLayerNetwork = if (networkFile.exists()) {
        ModelSerializer.restoreMultiLayerNetwork(networkFile)
    } else {
        createNetwork(hiddenLayerSizes)
    }
    private var epochCount = 0

    private fun createNetwork(hiddenLayerSizes: List<Int>): MultiLayerNetwork {
        val seed = 123L;

        val layerSizes = mutableListOf(inputSize)
        layerSizes.addAll(hiddenLayerSizes)

        val builder = NeuralNetConfiguration.Builder()
            .seed(seed)
            .dataType(DataType.DOUBLE)
            .l2(0.0005)
            .weightInit(WeightInit.XAVIER)
            .updater(Adam(1e-3))
            .list()

        for (i in 1 until layerSizes.size) {
            builder.layer(
                DenseLayer.Builder()
                    .activation(Activation.TANH)
                    .nIn(layerSizes[i-1])
                    .nOut(layerSizes[i])
                    .build()
            )
        }

        builder.layer(
                OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(layerSizes[layerSizes.size-1])
                    .nOut(outputSize)
                    .build()
            )

        val model = MultiLayerNetwork(builder.build())
        model.init()

        return model
    }

    fun train(inputDirectory: File, outputDirectory: File, suffixes: List<String> = listOf("png"), epochs: Int = 1, subEpochs: Int = 1, fileCount: Int = Int.MAX_VALUE) {
        assert(inputDirectory.isDirectory) { "is not a directory: $inputDirectory" }
        assert(outputDirectory.isDirectory) { "is not a directory: $outputDirectory" }
        val inputFiles = inputDirectory.listFiles { _, name -> suffixes.any { suffix -> name.endsWith(".$suffix") } }
        inputFiles.shuffle()

        for (i in 0 until epochs) {
            val dataSets = mutableListOf<DataSet>()

            for (inputFileIndex in 0 until kotlin.math.min(inputFiles.size, fileCount)) {
                val inputFile = inputFiles[inputFileIndex]
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

                println("Input file:  $inputFile")
                println("Output file: $outputFile")

                val (preprocessedInputImage, preprocessedOutputImage) = preprocessImages(inputImage, outputImage)
                val dataSetImages = toDataSetImages(preprocessedInputImage, preprocessedOutputImage)
                val preprocessedDataSetImages = preprocessDataSetImages(dataSetImages)
                val fileDataSets = toDataSets(preprocessedDataSetImages)
                dataSets.addAll(fileDataSets)
            }

            println("Epoch DataSet size ${dataSets.size}")
            dataSets.shuffle()
            val dataSetIterator = ListDataSetIterator(dataSets)
            doTrain(dataSetIterator, subEpochs)
        }
    }

    fun train(input: Image, output: Image, epochs: Int = 10) {
        val (preprocessedInputImage, preprocessedOutputImage) = preprocessImages(input, output)

        //ImageWriter.write(preprocessedInputImage, File("preprocessedInputImage.png"))
        //ImageWriter.write(preprocessedOutputImage, File("preprocessedOutputImage.png"))

        val preprocessedDataSetImages = preprocessDataSetImages(toDataSetImages(preprocessedInputImage, preprocessedOutputImage))
        val dataSets = preprocessDataSets(toDataSets(preprocessedDataSetImages))

        val dataSetIterator = ListDataSetIterator(dataSets)
        doTrain(dataSetIterator, epochs)
    }

    private fun doTrain(dataSetIterator: DataSetIterator, epochs: Int) {
        model.fit(dataSetIterator, epochs)

        epochCount += epochs
        File("score.txt").appendText("$epochCount, ${model.score()}\n")
        println("Score: " + model.score())

        model.save(networkFile, true)
    }

    fun test(input: Image, output: Image) {
        val dataSetImages = toDataSetImages(input, output)
        val dataSets = toDataSets(dataSetImages)
        val dataSetIterator = ListDataSetIterator(dataSets)

        val evaluation: org.nd4j.evaluation.regression.RegressionEvaluation = model.evaluateRegression(dataSetIterator)
        evaluation.columnNames = channels.map { c -> c.toString() }
        println(evaluation.stats())
    }

    private fun toDataSetImages(input: Image, output: Image): List<Pair<Image, Image>> {
        val result = mutableListOf<Pair<Image, Image>>()
        for (y in 0 until input.height) {
            for (x in 0 until input.width) {
                val inputSub = input.crop(x-offsetX, y-offsetY, inputWidth, inputHeight)
                val outputSub = output.crop(x, y, outputWidth, outputHeight)
                result.add(Pair(inputSub, outputSub))
            }
        }
        return result
    }

    private fun preprocessImages(input: Image, output: Image): Pair<Image, Image> {
        var transformedInput = input
        var transformedOutput = output

        for (imagePreProcessor in imagePreProcessors) {
            val transformed = imagePreProcessor.process(transformedInput, transformedOutput)
            transformedInput = transformed.first
            transformedOutput = transformed.second
        }

        //ImageWriter.write(transformedInput, File("input_preprocessed.png"))
        //ImageWriter.write(transformedOutput, File("output_preprocessed.png"))

        return Pair(transformedInput, transformedOutput)
    }

    private fun preprocessDataSetImages(dataSetImages: List<Pair<Image, Image>>): List<Pair<Image, Image>> {
        var result = dataSetImages
        println("Before dataset image preprocessing: ${result.size} datasets" )

        for (dataSetImagePreProcessor in dataSetImagePreProcessors) {
            result = dataSetImagePreProcessor.process(result)
            println("After ${dataSetImagePreProcessor}: ${result.size} datasets" )
        }

        println("After dataset image preprocessing: ${result.size} datasets" )

        ImageWriter.write(result[result.size/2].first, File("input_dataset_preprocessed.png"))
        ImageWriter.write(result[result.size/2].second, File("output_dataset_preprocessed.png"))

        return result
    }

    private fun preprocessDataSets(dataSets: List<DataSet>): List<DataSet> {
        var result = dataSets
        println("Before dataset preprocessing: ${result.size} datasets" )

        for (dataSetPreProcessor in dataSetPreProcessors) {
            result = dataSetPreProcessor.process(result)
            println("After ${dataSetPreProcessor}: ${result.size} datasets" )
        }

        println("After dataset preprocessing: ${result.size} datasets" )
        return result
    }

    private fun toDataSets(dataSetImages: List<Pair<Image, Image>>): MutableList<DataSet> {
        val dataSets = mutableListOf<DataSet>()

        for (dataSet in dataSetImages) {
            dataSets.add(toDataSet(dataSet.first, dataSet.second))
        }

        dataSets.shuffle()
        return dataSets
    }

    private fun toDataSets(input: Image, output: Image, pixelProbability: Double = 1.0, badResultBiasFactor: Double = 1.0, oversampleMinorityResults: Boolean = false): MutableList<DataSet> {
        val dataSets = mutableListOf<DataSet>()
        if (badResultBiasFactor < 1.0) {
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
            val minoritySize = clamp((dataSetsWithDistance.size * badResultBiasFactor + 0.5).toInt(), 1, dataSetsWithDistance.size)
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
        println("Corrected dataset size: ${dataSets.size}")

        return dataSets
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

        val inputShape = intArrayOf(inputSize)
        val outputShape = intArrayOf(outputSize)
        val inputNdArray: INDArray = Nd4j.create(inputShape, inputArray)
        val outputNdArray: INDArray = Nd4j.create(outputShape, outputArray)

        return DataSet(inputNdArray, outputNdArray)
    }

    fun run(input: Image, normalizeMin: Boolean = false, normalizeMax: Boolean = false): Image {
        val inputShape = intArrayOf(inputSize)

        /*
        return if (normalizeMin && normalizeMax) {
            val minValue = input.values().minOrNull()!!
            val maxValue = input.values().maxOrNull()!!
            val normalizedInput = input.copy().onEach { v -> (v - minValue) / (maxValue - minValue) }
            val output = doRun(normalizedInput)
            output.copy().onEach { v -> clamp(v * (maxValue - minValue) + minValue, 0.0, 1.0) }
        } else if (normalizeMin) {
            val minValue = input.values().minOrNull()!!
            val normalizedInput = input.elementMinus(minValue)
            val output = doRun(normalizedInput)
            output.copy().onEach { v -> clamp(v + minValue, 0.0, 1.0) }
        } else if (normalizeMax) {
            val maxValue = input.values().maxOrNull()!!
            val normalizedInput = input / maxValue
            val output = doRun(normalizedInput)
            output.copy().onEach { v -> clamp(v * maxValue, 0.0, 1.0) }
        } else {
         */

        val output = MatrixImage(input.width, input.height, channels)
        for (y in 0 until output.height step outputHeight) {
            for (x in 0 until output.width step outputWidth) {
                var inputSub = input.crop(x-offsetX, y-offsetY, inputWidth, inputHeight)
                var minValue = 0.0
                var maxValue = 1.0
                if (normalizeMin && normalizeMax) {
                    minValue = inputSub.values().minOrNull() ?: 0.0
                    maxValue = inputSub.values().maxOrNull() ?: 1.0
                    inputSub = inputSub.copy().onEach { v -> (v - minValue) / (maxValue - minValue) }
                } else if (normalizeMin) {
                    minValue = inputSub.values().minOrNull() ?: 0.0
                    inputSub = inputSub.elementMinus(minValue)
                } else if (normalizeMax) {
                    maxValue = inputSub.values().maxOrNull() ?: 1.0
                    inputSub = inputSub / maxValue
                }

                val inputArray = DoubleArray(inputSize) { i ->
                    val pixelIndex = i / channels.size
                    val channelIndex = i % channels.size
                    inputSub[channels[channelIndex]][pixelIndex]
                }
                val inputNdArray: INDArray = Nd4j.create(inputShape, inputArray)
                val outputNdArray = model.output(inputNdArray)
                val outputArray = outputNdArray.toDoubleVector()

                if (normalizeMin && normalizeMax) {
                    for(i in outputArray.indices) {
                        outputArray[i] = outputArray[i] * (maxValue - minValue) + minValue
                    }
                    //outputArray.onEach { v -> v * (maxValue - minValue) + minValue }
                } else if (normalizeMin) {
                    for(i in outputArray.indices) {
                        outputArray[i] = outputArray[i] + minValue
                    }
                    //outputArray.onEach { v -> v + minValue }
                } else if (normalizeMax) {
                    for(i in outputArray.indices) {
                        outputArray[i] = outputArray[i] * maxValue
                    }
                    //outputArray.onEach { v -> v * maxValue }
                } else {
                    outputArray
                }

                for (yOutput in 0 until outputHeight) {
                    for (xOutput in 0 until outputWidth) {
                        for (channelIndex in channels.indices) {
                            val outputIndex = (xOutput + yOutput * outputWidth) * channels.size + channelIndex
                            output[channels[channelIndex]][x+xOutput, y+yOutput] = clamp(outputArray[outputIndex], 0.0, 1.0)
                        }
                    }
                }
            }
        }

        return output
    }
}

interface ImagePreProcessor {
    fun process(input: Image, output: Image): Pair<Image, Image>
}

data class ImageNormalizePreProcessor(
    val normalizeMin: Boolean = true,
    val normalizeMax: Boolean = true
    ) : ImagePreProcessor {
    override fun process(input: Image, output: Image): Pair<Image, Image> {
        val minInput = if (normalizeMin) input.values().minOrNull() else null
        val maxInput = if (normalizeMax) input.values().maxOrNull() else null

        val minOutput = if (normalizeMin) output.values().minOrNull() else null
        val maxOutput = if (normalizeMin) output.values().minOrNull() else null

        val minValue = kotlin.math.min(minInput ?: 0.0, minOutput ?: 0.0)
        val maxValue = kotlin.math.max(maxInput ?: 1.0, maxOutput ?: 1.0)

        return if (normalizeMin && normalizeMax) {
            val scaledInput = input.copy().onEach { v -> (v - minValue) / (maxValue - minValue) }
            val scaledOutput = output.copy().onEach { v -> (v - minValue) / (maxValue - minValue) }
            Pair(scaledInput, scaledOutput)
        } else if (normalizeMin) {
            val scaledInput = input.elementMinus(minValue)
            val scaledOutput = output.elementMinus(minValue)
            Pair(scaledInput, scaledOutput)
        } else if (normalizeMax) {
            val scaledInput = input / maxValue
            val scaledOutput = output / maxValue
            Pair(scaledInput, scaledOutput)
        } else {
            Pair(input, output)
        }
    }
}

data class ImageMirrorPreProcessor(private val random: Random = Random()): ImagePreProcessor {
    override fun process(input: Image, output: Image): Pair<Image, Image> {
        val r = random.nextInt(3)
        val transformedInput = when(r) {
            1 -> input.mirrorX()
            2 -> input.mirrorY()
            else -> input
        }
        val transformedOutput = when(r) {
            1 -> output.mirrorX()
            2 -> output.mirrorY()
            else -> output
        }
        return Pair(transformedInput, transformedOutput)
    }
}

data class ImageRotate90PreProcessor(private val random: Random = Random()): ImagePreProcessor {
    override fun process(input: Image, output: Image): Pair<Image, Image> {
        val r = random.nextInt(4)
        val transformedInput = when(r) {
            1 -> input.rotateLeft()
            2 -> input.rotateRight()
            3 -> input.rotateRight().rotateRight()
            else -> input
        }
        val transformedOutput = when(r) {
            1 -> input.rotateLeft()
            2 -> input.rotateRight()
            3 -> input.rotateRight().rotateRight()
            else -> input
        }
        return Pair(transformedInput, transformedOutput)
    }
}

interface DataSetImagePreProcessor {
    fun process(dataSetImages: List<Pair<Image, Image>>): List<Pair<Image, Image>>
}

data class RandomDataSetImagePreProcessor(private val probability: Double, private val random: Random = Random()) : DataSetImagePreProcessor {
    override fun process(dataSetImages: List<Pair<Image, Image>>): List<Pair<Image, Image>> {
        if (probability >= 1.0) {
            return dataSetImages
        }

        val result = mutableListOf<Pair<Image, Image>>()
        for (dataSet in dataSetImages) {
            if (random.nextDouble() <= probability) {
                result.add(dataSet)
            }
        }
        return result
    }
}

data class GenericDataSetImagesPreProcessor(private val imagePreProcessor: ImagePreProcessor) : DataSetImagePreProcessor {
    override fun process(dataSetImages: List<Pair<Image, Image>>): List<Pair<Image, Image>> {
        val result = mutableListOf<Pair<Image, Image>>()
        for (dataSet in dataSetImages) {
            result.add(imagePreProcessor.process(dataSet.first, dataSet.second))
        }
        return result
    }
}

data class SampleBiasPreProcessor(private val accuracy: Double = 0.001) : DataSetImagePreProcessor {
    override fun process(dataSetImages: List<Pair<Image, Image>>): List<Pair<Image, Image>> {
        val result = mutableListOf<Pair<Image, Image>>()

        val groupedDataSets = dataSetImages.groupBy {  d -> toGroupKey(d.second) }

        val minSize = groupedDataSets.values.map { v -> v.size }.minOrNull()
        val maxSize = groupedDataSets.values.map { v -> v.size }.maxOrNull()

        if (maxSize != null && minSize != null) {
            val targetSize = kotlin.math.max(minSize, (maxSize + minSize) / 2)

            for ((key, group) in groupedDataSets.entries) {
                if (group.size > targetSize) {
                    result.addAll(group.subList(0, targetSize))
                } else if (group.size < targetSize) {
                    val oversampleSize = targetSize - group.size
                    val nGroups = (oversampleSize / group.size)
                    val nSets = (oversampleSize % group.size)
                    for (i in 0 until nGroups) {
                        result.addAll(group)
                    }
                    for (i in 0 until nSets) {
                        result.add(group[i])
                    }
                }
            }
        }
        return result
    }

    private fun toGroupKey(output: Image): List<Int> {
        val result = mutableListOf<Int>()
        for (channel in output.channels) {
            for (y in 0 until output.height) {
                for (x in 0 until output.width) {
                    result.add(toValueBin(output[x, y, channel]))
                }
            }
        }
        return result
    }

    private fun toValueBin(value: Double): Int {
        return (value / accuracy).toInt()
    }
}

interface DataSetPreProcessor {
    fun process(dataSet: List<DataSet>): List<DataSet>
}

